#!/usr/bin/env ruby

#require 'thin'
require 'sinatra/base'
require 'json'

require_relative 'tenantmanager'
require_relative 'provisioner'

module Loom
  class ProvisionerApi < Sinatra::Base

    attr_accessor :provisioner

    def initialize()
      $stdout.sync = true
      super()
      setup_signal_traps
      @provisioner = Loom::Provisioner.new
      spawn_heartbeat_thread
      spawn_signal_thread
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
          puts "reaping #{$signals.size} signals: #{$signals}" unless $signals.empty?
          signals_processed = {}
          while !$signals.empty?
            sig = $signals.shift
            next if signals_processed.key?(sig)
            puts "processing signal: #{sig}"
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
                puts "provisioner shutdown complete"
                exit
              end
            end
            puts "done processing signal #{sig}"
            signals_processed[sig] = true
          end
        sleep 10
        }
      }
    end

    def spawn_heartbeat_thread
      Thread.new {
        loop {
          puts @provisioner.heartbeat.to_json
          sleep 10
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
      puts "total ruby threads: #{Thread.list.size}"
      @provisioner.status
      body "OK"
    end

    get '/heartbeat' do
      @provisioner.heartbeat.to_json
    end

    post "/v1/tenants" do
      puts "adding tenant"
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
      puts "adding/updating tennant id: #{params[:t_id]}"
      data = JSON.parse request.body.read 
      workers = data['workers'] || 3 # TO DO: replace default with constant
      puts "requesting workers: #{workers}"
      modules = data['modules'] || nil
      puts "requesting modules: #{modules}"
      plugins = data['plugins'] || nil
      puts "requesting plugins: #{plugins}"

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

    # replace with start script
    run! if app_file == $0
  end
end

