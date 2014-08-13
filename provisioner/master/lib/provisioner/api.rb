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

require_relative 'tenantspec'
require_relative 'provisioner'
require_relative 'cli'
require_relative 'logging'

module Loom
  class Provisioner
    # class to define provisioner API
    class Api < Sinatra::Base
      include Logging

      set :environment, :production

      get '/status' do
        body 'OK'
      end

      get '/heartbeat' do
        begin
          settings.provisioner.heartbeat.to_json
        rescue
          halt 500
        end
      end

      post "/#{PROVISIONER_API_VERSION}/tenants" do
        begin
          log.info 'adding tenant'
          data = JSON.parse request.body.read
          id = data['id']
          workers = data['workers']
          resources = data['resources'] || nil
          plugins = data['plugins'] || nil

          ts = TenantSpec.new(id, workers, resources, plugins)

          settings.provisioner.add_tenant(ts)

          data['status'] = 0
          body data.to_json
        rescue
          halt 500
        end
      end

      put "/#{PROVISIONER_API_VERSION}/tenants/:t_id" do
        begin
          log.info "adding/updating tenant id: #{params[:t_id]}"
          data = JSON.parse request.body.read
          workers = data['workers'] || 0
          log.debug "requesting workers: #{workers}"
          resources = data['resources'] || nil
          log.debug "requesting resources: #{resources}"
          plugins = data['plugins'] || nil
          log.debug "requesting plugins: #{plugins}"

          ts = TenantSpec.new(params[:t_id], workers, resources, plugins)

          settings.provisioner.add_tenant(ts)

          data['status'] = 0
          body data.to_json
        rescue
          halt 500
        end
      end

      delete "/#{PROVISIONER_API_VERSION}/tenants/:t_id" do
        begin
          if settings.provisioner.tenantmanagers.key?(params[:t_id])
            settings.provisioner.delete_tenant(params[:t_id])
            body 'OK'
          else
            halt 404
          end
        rescue
          halt 500
        end
      end
    end
  end
end
