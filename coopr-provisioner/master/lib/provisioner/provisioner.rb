#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'thin'
require 'sinatra/base'
require 'json'
require 'rest_client'
require 'socket'
require 'resolv'

require_relative 'api'
require_relative 'tenantmanager'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'
require_relative 'config'
require_relative 'constants'
require_relative 'workerlauncher'
require_relative 'rest-helper'

module Coopr
  # top-level class for provisioner
  class Provisioner
    include Logging

    attr_accessor :tenantmanagers, :provisioner_id, :server_uri

    def initialize(options, config)
      @options = options
      @config = config
      @tenantmanagers = {}
      @terminating_tenants = []
      @server_uri = config.get('provisioner.server.uri')
      pid = Process.pid
      host = Socket.gethostname.downcase.split('.').first
      @provisioner_id = "master-#{host}-#{pid}"
      log.info "provisioner #{@provisioner_id} initialized"
      @registered = false
      Logging.process_name = @provisioner_id

      pem_path = config.get('trust.cert.path')
      pem_pass = config.get('trust.cert.pass')
      @rest_helper = Coopr::RestHelper.new(pem_path, pem_pass)
    end

    # invoked from bin/provisioner
    def self.run(options)
      # read configuration
      config = Config.new(options)
      config.load

      # initialize logging
      Logging.configure(config.get(PROVISIONER_LOG_DIR) ? "#{config.get(PROVISIONER_LOG_DIR)}/provisioner.log" : nil)
      Logging.level = config.get(PROVISIONER_LOG_LEVEL)
      Logging.shift_age = config.get(PROVISIONER_LOG_ROTATION_SHIFT_AGE)
      Logging.shift_size = config.get(PROVISIONER_LOG_ROTATION_SHIFT_SIZE)
      Logging.log.debug 'Provisioner starting up'
      config.properties.each do |k, v|
        Logging.log.debug "  #{k}: #{v}"
      end

      # daemonize
      daemonize if config.get(PROVISIONER_DAEMONIZE)

      pg = Coopr::Provisioner.new(options, config)
      if options[:register]
        pg.register_plugins
      else
        pg.run
      end
    end

    # main run block
    def run
      begin
        @status = 'STARTING'
        Thread.abort_on_exception = true
        # start the api server
        spawn_sinatra_thread
        # wait for sinatra to fully initialize
        sleep 1 until Api.running?
        # register our own signal handlers
        setup_signal_traps
        # spawn the heartbeat, signal-handler, and resource threads
        spawn_heartbeat_thread
        spawn_signal_thread
        spawn_resource_thread

        # heartbeat thread will update status to 'OK'

        # wait for signal_handler to exit in response to signals
        @signal_thread.join
        # kill the other threads
        [@heartbeat_thread, @sinatra_thread, @resource_thread].each do |t|
          t.kill
          t.join
        end
        log.info "provisioner gracefully shut down"
        exit
      rescue RuntimeError => e
        log.error "Exception raised in thread: #{e.inspect}, shutting down..."
        # if signal_handler thread alive, use it to shutdown gracefully
        if @signal_thread && @signal_thread.alive?
          Process.kill('TERM', 0)
          @signal_thread.join
          [@heartbeat_thread, @sinatra_thread, @resource_thread].each do |t|
            t.kill if t.alive?
          end
          log.info "provisioner forced graceful shutdown"
          exit 1
        else
          # last resort, kill entire process group
          Process.kill('TERM', -Process.getpgrp)
          log.info "provisioner forced shutdown"
          exit 1
        end
      end
    end

    def spawn_sinatra_thread
      @sinatra_thread = Thread.new {
        # set reference to provisioner
        Api.set :provisioner, self
        # set bind settings
        bind_ip = @config.get(PROVISIONER_BIND_IP)
        bind_port = @config.get(PROVISIONER_BIND_PORT)
        Api.set :bind, bind_ip
        Api.set :port, bind_port
        # let sinatra take over from here
        Api.run!
      }

    end

    def setup_signal_traps
      @signals = Array.new
      %w(CLD TERM INT).each do |signal|
        Signal.trap(signal) do
          @signals << signal
        end
      end
    end

    def spawn_signal_thread
      @signal_thread = Thread.new {
        log.info "started signal processing thread"
        loop {
          log.debug "reaping #{@signals.size} signals: #{@signals}" unless @signals.empty?
          signals_processed = {}
          unless @signals.empty?
            sig = @signals.shift
            next if signals_processed.key?(sig)
            log.debug "processing signal: #{sig}"
            case sig
            when 'CLD'
              verify_tenants
            when 'TERM', 'INT'
              unless @shutting_down
                # begin shutdown procedure
                @shutting_down = true
                tenantmanagers.each do |k, v|
                  v.delete
                end
                # wait for all workers to shut down
                Process.waitall
                unregister_from_server
                # exit thread
                Thread.current.kill
              end
            end
            signals_processed[sig] = true
          end
          sleep 1
        }
      }
    end

    def spawn_heartbeat_thread
      @heartbeat_thread = Thread.new {
        log.info "starting heartbeat thread"
        loop {
          register_with_server unless @registered
          uri = "#{@server_uri}/v2/provisioners/#{provisioner_id}/heartbeat"
          begin
            json = heartbeat.to_json
            resp = @rest_helper.post("#{uri}", json, :'Coopr-UserID' => "admin")
            unless resp.code == 200
              if(resp.code == 404)
                log.warn "Response code #{resp.code} when sending heartbeat, re-registering provisioner"
                register_with_server
              else
                log.warn "Response code #{resp.code}, #{resp.to_str} when sending heartbeat to coopr server #{uri}"
              end
            end
          rescue => e
            log.error "Caught exception sending heartbeat to coopr server #{uri}: #{e.message}"
          end
          sleep 10
        }
      }
    end

    def spawn_resource_thread
      @resource_thread = Thread.new {
        log.info "starting resource thread"
        loop {
          @tenantmanagers.each do |id, tmgr|
            if tmgr.resource_sync_needed? && tmgr.num_workers == 0
              # handle stacked sync calls, last one wins
              while tmgr.resource_sync_needed?
                log.debug "resource thread invoking sync for tenant #{tmgr.id}"
                tmgr.sync
              end
              log.debug "done syncing tenant #{tmgr.id}, resuming workers"
              tmgr.resume
            end
          end
          sleep 1
        }
      }
    end

    def register_with_server
      uri = "#{@server_uri}/v2/provisioners/#{@provisioner_id}"
      data = {}
      data['id'] = @provisioner_id
      data['capacityTotal'] = @config.get(PROVISIONER_CAPACITY)
      data['host'] = @config.get(PROVISIONER_REGISTER_IP) || local_ip
      data['port'] = @config.get(PROVISIONER_BIND_PORT)

      log.info "Registering with server at #{uri}: #{data.to_json}"

      begin
        resp = @rest_helper.put("#{uri}", data.to_json, :'Coopr-UserID' => "admin")
        if(resp.code == 200)
          log.info "Successfully registered"
          @registered = true
          # announce provisioner is ready
          @status = 'OK'
        else
          log.warn "Response code #{resp.code}, #{resp.to_str} when registering with coopr server #{uri}"
        end
      rescue => e
        log.error "Caught exception when registering with server #{uri}: #{e.message}"
      end
    end

    def register_plugins
      worker_launcher = WorkerLauncher.new(@config)
      worker_launcher.provisioner = @provisioner_id
      worker_launcher.name = "plugin-registration-worker"
      worker_launcher.register = true
      worker_cmd = worker_launcher.cmd
      log.debug "launching worker to register plugins: #{worker_cmd}"
      exec(worker_cmd)
    end

    def unregister_from_server
      uri = "#{@server_uri}/v2/provisioners/#{@provisioner_id}"
      log.info "Unregistering with server at #{uri}"
      begin
        resp = @rest_helper.delete("#{uri}", :'Coopr-UserID' => "admin")
        if(resp.code == 200)
          log.info "Successfully unregistered"
        else
          log.warn "Response code #{resp.code}, #{resp.to_str} when unregistering with coopr server #{uri}"
        end
      rescue => e
        log.error "Caught exception when unregistering with coopr server #{uri}: #{e.message}"
      end
    end

    def self.daemonize
      Process.daemon
    end

    # api method to add or edit tenant
    def add_tenant(tenantspec)
      unless tenantspec.instance_of?(TenantSpec)
        raise ArgumentError, "only instances of TenantSpec can be added to provisioner", caller
      end
      # validate input
      id = tenantspec.id
      log.debug "Adding/Editing tenant: #{id}"
      fail "cannot add a TenantManager without an id: #{tenantmgr.inspect}" if id.nil?

      tenantmgr = TenantManager.new(tenantspec, @config, @provisioner_id)

      if @tenantmanagers.key? id
        # edit tenant
        log.debug "Editing tenant: #{id}"
        @tenantmanagers[id].update(tenantmgr)
      else
        # new tenant
        log.debug "Adding new tenant: #{id}"
        #tenantmgr.spawn
        tenantmgr.resource_sync_needed
        @tenantmanagers[id] = tenantmgr
      end
    end

    # api method to delete tenant for given id
    def delete_tenant(id)
      # if no workers currently running, just delete
      if @tenantmanagers[id].num_workers == 0
        @tenantmanagers.delete(id)
      else
        # instruct tenant to send kill signal to its workers
        @tenantmanagers[id].delete
        # we mark this tenant as deleting.  when its num_workers reaches 0 it can be deleted from @tenantmanagers
        @terminating_tenants.push(id)
      end
    end

    # check running tenants and their workers, called after a CLD signal processed
    def verify_tenants
      @tenantmanagers.each do |k, v|
        # update worker counts
        v.verify_workers
        # has this tenant been deleted?
        if (@terminating_tenants.include?(k) && v.num_workers == 0)
          @tenantmanagers.delete(k)
          @terminating_tenants.delete(k)
        end
      end
    end

    # get current heartbeat data
    def heartbeat
      hb = {}
      hb['usage'] = {}
      @tenantmanagers.each do |id, tm|
        hb['usage'][id] = tm.num_workers
      end
      hb
    end

    # get current status
    def status
      @status ||= 'UNKNOWN'
    end

    # determine ip to register with server from routing info
    # http://coderrr.wordpress.com/2008/05/28/get-your-local-ip-address/
    def local_ip
      begin
        server_ip = Resolv.getaddress( @server_uri.sub(%r{^https?://}, '').split(':').first ) rescue '127.0.0.1'
        orig, Socket.do_not_reverse_lookup = Socket.do_not_reverse_lookup, true # turn off reverse DNS resolution temporarily
        UDPSocket.open do |s|
          s.connect server_ip, 1
          s.addr.last
        end
      rescue => e
        log.error "Unable to determine provisioner.register.ip, defaulting to 127.0.0.1. Please set it explicitly. "\
          "Server may not be able to connect to this provisioner: #{e.inspect}"
        '127.0.0.1'
      ensure
        Socket.do_not_reverse_lookup = orig
      end
    end
  end
end
