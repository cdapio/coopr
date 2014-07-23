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

require_relative 'tenantmanager'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'

module Loom
  class Provisioner
    class Api < Sinatra::Base
      include Logging

      set :environment, :production

      get '/status' do
        body "OK"
      end

      get '/heartbeat' do
        log.info "heartbeat called"
        settings.provisioner.heartbeat.to_json
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

        settings.provisioner.add_tenant(tm)

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

        settings.provisioner.add_tenant(tm)

        data['status'] = 0
        body data.to_json
      end

      delete "/v1/tenants/:t_id" do
        if settings.provisioner.tenantmanagers.key?(params[:t_id])
          settings.provisioner.delete_tenant(params[:t_id])
          body "OK"
        else
          halt 404
        end
      end
    end
  end
end

