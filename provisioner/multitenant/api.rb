#!/usr/bin/env ruby

require 'thin'
require 'sinatra/base'
require 'json'

require_relative 'tenantmanager'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'

module Loom
  class ProvisionerApi < Sinatra::Base
    include Logging

    attr_accessor :provisioner

    def initialize()
#      @logger = Logger.new('/Users/derek/git/loom/provisioner/multitenant/log-api.log')
      $stdout.sync = true
      super()
      setup_signal_traps
      @provisioner = Loom::Provisioner.new
      spawn_heartbeat_thread
      spawn_signal_thread
    end

    def self.run(options)
      Logging.configure(options[:log_file])
      Logging.level = options[:log_level]
      Logging.log.info "Loom api starting up" 

#      puts "setup traps"
#      setup_signal_traps
#      puts "create provisioner"
#      @provisioner = Loom::Provisioner.new
#      puts "spawn hb"
#      spawn_heartbeat_thread
#      puts "spawn reap"
#      spawn_signal_thread
#      configure(options[:log_file])
#      level = options[:log_level]
      #Logging.level = 0

#      EM::run do
        #$stdout.sync = true
        setup_process if options[:daemonize]
        #Thin::Logging.silent = true
        bind_address = '0.0.0.0'
        #Thin::Server.start(bind, '4567', self, :signals => false)
        Thin::Server.start(bind_address, '4567', self)
#      end

      Logging.log.info "after start - does it ever unblock?"
    #end

      #self.run!
#      run!
#      puts "after run!"

#      loop {
#        puts "looping"
#        sleep 10
#      }
    end

    def setup_signal_traps
      $signals = Array.new
      ['CLD', 'TERM'].each do |signal|
        Signal.trap(signal) do
          $signals << signal
        end
      end
    end

    def spawn_signal_thread
      Thread.new {
        loop {
          log.info "reaping #{$signals.size} signals: #{$signals}" unless $signals.empty?
          signals_processed = {}
          while !$signals.empty?
            sig = $signals.shift
            next if signals_processed.key?(sig)
            log.info "processing signal: #{sig}"
            case sig
            when 'CLD'
              @provisioner.tenantmanagers.each do |k, v|
                v.verify_children
              end
            when 'TERM'
              if !@shutting_down
                @shutting_down = true
                @provisioner.tenantmanagers.each do |k, v|
                  v.terminate_all_worker_processes
                end
                Process.waitall
                log.info "provisioner shutdown complete"
                exit
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
      Thread.new {
        loop {
          log.info @provisioner.heartbeat.to_json
          sleep 5
        }
      }
    end

    get '/hi' do
      "Hello World!"
    end

    get '/hello/:name' do
      # matches "GET /hello/foo" and "GET /hello/bar"
      # params[:name] is 'foo' or 'bar'
      "Hello #{params[:name]}!"
    end

    get '/status' do
      @provisioner.status
      body "OK"
    end

    get '/heartbeat' do
      log.info "heartbeat called"
      @provisioner.heartbeat.to_json
    end

    post "/v1/tenants" do
      log.info "adding tenant"
      data = JSON.parse request.body.read
      id = data['id']
      workers = data['workers']
      modules = data['modules'] || nil
      plugins = data['plugins'] || nil

      ts = TenantSpec.new(id, workers, modules, plugins)
      tm = TenantManager.new(ts)

      @provisioner.add_tenant(tm)

      data['status'] = 0
      body data.to_json
    end

    put "/v1/tenants/:t_id" do
      log.info "adding/updating tennant id: #{params[:t_id]}"
      data = JSON.parse request.body.read 
      workers = data['workers'] || 3 # TO DO: replace default with constant
      log.debug "requesting workers: #{workers}"
      modules = data['modules'] || nil
      log.debug "requesting modules: #{modules}"
      plugins = data['plugins'] || nil
      log.debug "requesting plugins: #{plugins}"

      ts = TenantSpec.new(params[:t_id], workers, modules, plugins)
      tm = TenantManager.new(ts)

      @provisioner.add_tenant(tm)
#      provisioner = provisioner.getinstance
#      provisioner.add_tenant(tm)

#      tm.spawn

      data['status'] = 0

      #data = JSON.parse(params[:data])
      body data.to_json
    end

    delete "/v1/tenants/:t_id" do
      @provisioner.delete_tenant(params[:t_id])
      body "OK"
    end

    def self.setup_process
      Process.daemon
    #  process = Process.new
    #  if @options[:daemonize]
    #    process.daemonize
    #  end
    end


    # replace with start script
    run! if app_file == $0
  end
end

