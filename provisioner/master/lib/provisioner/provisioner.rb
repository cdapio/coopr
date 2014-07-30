#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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

module Loom
  class Provisioner
    include Logging

    attr_accessor :tenantmanagers, :provisioner_id, :server_uri

    def initialize(options, config)
      @options = options
      @config = config
      @tenantmanagers = {}
      @terminating_tenants = []
      @server_uri = config.get_value('server.uri')
      pid = Process.pid
      host = Socket.gethostname.downcase
      @provisioner_id = "#{host}.#{pid}"
      log.info "provisioner #{@provisioner_id} initialized"
    end

    # invoked from bin/provisioner
    def self.run(options)

      # read configuration
      config = Config.new(options)
      config.load_default

      # initialize logging
      Logging.configure(config.get_value('log.directory') ? "#{config.get_value('log.directory')}/provisioner.log" : nil)
      Logging.level = config.get_value('log.level')
      Logging.log.info "Loom api starting up"

      # daemonize
      daemonize if options[:daemonize]

      pg = Loom::Provisioner.new(options, config)
      if options[:register]
        pg.register_plugins
      else
        pg.run
      end
    end

    # main run block
    def run
      # start the api server
      spawn_sinatra_thread
      # wait for sinatra to fully initialize
      sleep 1 until Api.running?
      # register our own signal handlers
      setup_signal_traps
      # spawn the heartbeat thread
      spawn_heartbeat_thread
      # spawn the signal handler thread
      spawn_signal_thread

      # wait for signal_handler to exit in response to signals
      @signal_thread.join
      # kill the other threads
      @heartbeat_thread.kill
      @sinatra_thread.kill
      @heartbeat_thread.join
      @sinatra_thread.join
      log.info "provisioner gracefully shut down"
      exit
    end

    def spawn_sinatra_thread
      @sinatra_thread = Thread.new {
        # set reference to provisioner
        Api.set :provisioner, self
        # set bind settings
        bind_ip = @config.get_value('bind.ip')
        bind_port = @config.get_value('bind.port')
        Api.set :bind, bind_ip
        Api.set :port, bind_port
        # let sinatra take over from here
        Api.run!
      }
      # surface any exceptions immediately
      @sinatra_thread.abort_on_exception=true

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
        register_with_server
        loop {
          uri = "#{@server_uri}/v1/provisioners/#{provisioner_id}/heartbeat"
          begin
            json = heartbeat.to_json
            resp = RestClient.post("#{uri}", json, :'X-Loom-UserID' => "admin")
            unless resp.code == 200
              if(resp.code == 404)
                log.warn "Response code #{resp.code} when sending heartbeat, re-registering provisioner"
                register_with_server
              else
                log.warn "Response code #{resp.code}, #{resp.to_str} when sending heartbeat to loom server #{uri}"
              end
            end
          rescue => e
            log.error "Caught exception sending heartbeat to loom server #{uri}: #{e.message}"
          end
          sleep 10
        }
      }
      # abort on any uncaught exception during registration, etc
      @heartbeat_thread.abort_on_exception=true
    end

    def register_with_server
      uri = "#{@server_uri}/v1/provisioners/#{@provisioner_id}"
      data = {}
      data['id'] = @provisioner_id
      data['capacityTotal'] = @config.get_value('default.capacity')
      data['host'] = @config.get_value('register.ip') || local_ip
      data['port'] = @config.get_value('bind.port')

      log.info "Registering with server at #{uri}: #{data.to_json}"

      begin
        resp = RestClient.put("#{uri}", data.to_json, :'X-Loom-UserID' => "admin")
        if(resp.code == 200)
          log.info "Successfully registered"
        else
          log.warn "Response code #{resp.code}, #{resp.to_str} when registering with loom server #{uri}"
        end
      rescue => e
        log.error "Caught exception when registering with server #{uri}: #{e.message}"
      end
    end

    # this is temporary until provisioner process manages worker data
    def register_plugins
      # launch a single worker with register flag
      exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} #{File.dirname(__FILE__)}/../../../worker/provisioner.rb --uri #{@server_uri} --register")
    end

    def unregister_from_server
      uri = "#{@server_uri}/v1/provisioners/#{@provisioner_id}"
      log.info "Unregistering with server at #{uri}"
      begin
        resp = RestClient.delete("#{uri}", :'X-Loom-UserID' => "admin")
        if(resp.code == 200)
          log.info "Successfully unregistered"
        else
          log.warn "Response code #{resp.code}, #{resp.to_str} when unregistering with loom server #{uri}"
        end
      rescue => e
        log.error "Caught exception when unregistering with loom server #{uri}: #{e.message}"
      end
    end

    def self.daemonize
      Process.daemon
    end

    # api method to add or edit tenant
    def add_tenant(tenantmgr)
      unless tenantmgr.instance_of?(TenantManager)
        raise ArgumentError, "only instances of TenantManager can be added to provisioner", caller
      end
      # validate input
      id = tenantmgr.id
      log.debug "Adding/Editing tenant: #{id}"
      fail "cannot add a TenantManager without an id: #{tenantmgr.inspect}" if id.nil?

      # set provisionerId
      tenantmgr.provisioner_id = @provisioner_id

      # set configuration
      tenantmgr.config = @config

      if @tenantmanagers.key? id
        # edit tenant
        log.debug "Editing tenant: #{id}"
        @tenantmanagers[id].update(tenantmgr)
      else
        # new tenant
        log.debug "Adding new tenant: #{id}"
        tenantmgr.spawn
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

    # determine ip to register with server from routing info
    # http://coderrr.wordpress.com/2008/05/28/get-your-local-ip-address/
    def local_ip
      server_ip = Resolv.getaddress( @server_uri.sub(%r{^https?://}, '').split(':').first ) rescue '127.0.0.1'
      orig, Socket.do_not_reverse_lookup = Socket.do_not_reverse_lookup, true # turn off reverse DNS resolution temporarily
      UDPSocket.open do |s|
        s.connect server_ip, 1
        s.addr.last
      end
    ensure
      Socket.do_not_reverse_lookup = orig
    end

  end
end
