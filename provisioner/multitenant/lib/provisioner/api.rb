#!/usr/bin/env ruby

require 'thin'
require 'sinatra/base'
require 'json'
require 'rest_client'

require_relative 'tenantmanager'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'

module Loom
  class Provisioner
    class Api < Sinatra::Base
      include Logging
      @@provisioner

      # used for testing
      def self::provisioner= (value)
        @@provisioner = value
      end

      def self.run_for_provisioner!(provisioner)
        @@provisioner = provisioner
        # sinatra blocks
        run!
      end

      set :logging, false

      get '/status' do
        body "OK"
      end

      get '/heartbeat' do
        log.info "heartbeat called"
        @@provisioner.heartbeat.to_json
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

        @@provisioner.add_tenant(tm)

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

        @@provisioner.add_tenant(tm)

        data['status'] = 0
        body data.to_json
      end

      delete "/v1/tenants/:t_id" do
        @@provisioner.delete_tenant(params[:t_id])
        body "OK"
      end
    end
  end
end

