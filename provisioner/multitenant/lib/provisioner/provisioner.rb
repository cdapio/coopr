#!/usr/bin/env ruby

require 'thin'
require 'sinatra/base'
require 'json'
require 'rest_client'

require_relative 'api'
require_relative 'tenantmanager'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'

module Loom
  class Provisioner
    include Logging

    attr_accessor :tenantmanagers, :provisioner_id, :server_uri
    @sinatra_thread
    @signal_thread
    @heartbeat_thread

    def initialize()
      @tenantmanagers = {}
      @terminating_tenants = []
      pid = Process.pid
      host = Socket.gethostname.downcase
      @provisioner_id = "#{host}.#{pid}"
      log.info "provisioner #{@provisioner_id} initialized"
    end

    # invoked from bin/provisioner
    def self.run(options)
      @server_uri = options[:uri]

      Logging.configure(options[:log_file])
      Logging.level = options[:log_level]
      Logging.log.info "Loom api starting up"

      #register_plugins if options[:register]

      setup_process if options[:daemonize]

      pg = Loom::Provisioner.new
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
      # kill the others
      @heartbeat_thread.kill
      @sinatra_thread.kill
      @heartbeat_thread.join
      @sinatra_thread.join
      log.info "provisioner shut down"
      exit
    end

    def spawn_sinatra_thread
      @sinatra_thread = Thread.new { Api.run_for_provisioner!(self) }
      # surface any exceptions immediately
      @sinatra_thread.abort_on_exception=true

    end

    def setup_signal_traps
      $signals = Array.new
      ['CLD', 'TERM', 'INT'].each do |signal|
        Signal.trap(signal) do
          $signals << signal
        end
      end
    end

    def spawn_signal_thread
      @signal_thread = Thread.new {
        log.info "started signal processing thread"
        loop {
          log.info "reaping #{$signals.size} signals: #{$signals}" unless $signals.empty?
          #Logging.log.info "reaping #{$signals.size} signals: #{$signals}"
          signals_processed = {}
          while !$signals.empty?
            sig = $signals.shift
            next if signals_processed.key?(sig)
            log.info "processing signal: #{sig}"
            case sig
            when 'CLD'
              verify_tenants
            when 'TERM', 'INT'
              if !@shutting_down
                @shutting_down = true
                tenantmanagers.each do |k, v|
                  v.terminate_all_worker_processes
                end
                Process.waitall
                log.info "workers shutdown"
                unregister_from_server
                Thread.current.kill
                #exit
              end
            end
            log.info "done processing signal #{sig}"
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
          #Logging.log.info "hbt: #{$provisioner.heartbeat.to_json}"
          uri = "#{@server_uri}/v1/provisioners/#{provisioner_id}/heartbeat"
          begin
            log.debug "sending heartbeat to #{uri}"
            json = heartbeat.to_json
            resp = RestClient.post("#{uri}", json, :'X-Loom-UserID' => "admin")
            if(resp.code == 200)
              log.debug "Successfully sent heartbeat"
            elsif(resp.code == 404)
              log.warn "Response code #{resp.code} when sending heartbeat, re-registering provisioner"
              register_with_server
            else
              log.warn "Response code #{resp.code}, #{resp.to_str} when sending heartbeat to loom server #{uri}"
            end
          rescue => e
            log.error "Caught exception sending heartbeat to loom server #{uri}: #{e.message}"
            #log.error e.message
            #log.error e.backtrace.inspect
          end
          sleep 10
        }
      }
    end

    def register_with_server
      uri = "#{@server_uri}/v1/provisioners/#{@provisioner_id}"
      data = {}
      data['id'] = @provisioner_id
      data['capacityTotal'] = '100'
      data['host'] = '127.0.0.1'
      data['port'] = '4567'

      Logging.log.info "Registering with server at #{uri}: #{data.to_json}"

      begin
        resp = RestClient.put("#{uri}", data.to_json, :'X-Loom-UserID' => "admin")
        if(resp.code == 200)
          Logging.log.info "Successfully registered"
        else
          Logging.log.warn "Response code #{resp.code}, #{resp.to_str} when registering with loom server #{uri}"
        end
      rescue => e
        Logging.log.error "Caught exception when registering with loom server #{uri}: #{e.message}"
        #log.error e.message
        #log.error e.backtrace.inspect
      end
    end

    # this is temporary
    def register_plugins
      exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} #{File.dirname(__FILE__)}/../daemon/provisioner.rb --uri #{@server_uri} --register")
    end

    def unregister_from_server
      uri = "#{@server_uri}/v1/provisioners/#{@provisioner_id}"
      Logging.log.info "Unregistering with server at #{uri}"
      begin
        resp = RestClient.delete("#{uri}", :'X-Loom-UserID' => "admin")
        if(resp.code == 200)
          Logging.log.info "Successfully unregistered"
        else
          Logging.log.warn "Response code #{resp.code}, #{resp.to_str} when unregistering with loom server #{uri}"
        end
      rescue => e
        Logging.log.error "Caught exception when unregistering with loom server #{uri}: #{e.message}"
        #log.error e.message
        #log.error e.backtrace.inspect
      end
    end

    def self.setup_process
      Process.daemon
    #  process = Process.new
    #  if @options[:daemonize]
    #    process.daemonize
    #  end
    end

    def add_tenant(tenantmgr)
      if !tenantmgr.instance_of?(TenantManager)
        raise ArgumentError, "only instances of TenantManager can be added to provisioner", caller
      end
      # validate input
      id = tenantmgr.id
      log.info "Adding/Editing tenant: #{id}"
      raise "cannot add a TenantManager without an id: #{tenantmgr.inspect}" if id.nil?

      # set provisionerId
      tenantmgr.provisioner_id = @provisioner_id

      if @tenantmanagers.key? id
        # edit tenant
        log.info "Editing tenant: #{id}"
        @tenantmanagers[id].update(tenantmgr)
      else
        # new tenant
        log.info "Adding new tenant: #{id}"
        tenantmgr.spawn
        @tenantmanagers[id] = tenantmgr
      end
    end

    def verify_tenants
      @tenantmanagers.each do |k, v|
        v.verify_workers
        # has this tenant been deleted?
        if (@terminating_tenants.include?(k) && v.num_workers == 0)
          @tenantmanagers.delete(k)
          @terminating_tenants.delete(k)
        end
      end
    end

    def delete_tenant(id)
      # if no workers running, just delete
      if @tenantmanagers[id].num_workers == 0
        @tenantmanagers.delete(id)
      else
        # instruct tenant to send kill signal to its workers
        @tenantmanagers[id].delete
        # we mark this tenant as deleting.  when its num_workers reaches 0 it can be deleted from @tenantmanagers
        @terminating_tenants.push(id)
      end
    end

    def status
      @tenantmanagers.each do |id, tm|
        tm.check_threads
      end
    end

    def heartbeat
      hb = {}
      hb['usage'] = {}
      @tenantmanagers.each do |id, tm|
        hb['usage'][id] = tm.num_workers
      end
      hb
    end

  end
end
