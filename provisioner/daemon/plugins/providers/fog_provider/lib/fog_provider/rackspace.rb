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

require_relative 'rackspace/create'

class FogProviderRackspace < FogProvider

  def create(inputmap)
    flavor = inputmap['flavor']
    image = inputmap['image']
    hostname = inputmap['hostname']
    fields = inputmap['fields']

    begin
      # Our fields are fog symbols
      fields.each do |k,v|
        k.to_sym = v
      end

      # Create the server
      log.debug 'Invoking server create'
      instance = FogProviderRackspaceCreate.new

    end
  end

  def confirm(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']
  end

  def delete(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']
  end

  def connection
    log.debug "Connection options for Rackspace:"
    log.debug "rackspace_version #{rackspace_version}"
    log.debug "rackspace_api_key #{rackspace_api_key}"
    log.debug "rackspace_username #{rackspace_username}"
    log.debug "rackspace_region #{rackspace_region}"

    # Create connection
    @connection ||= begin
      connection = Fog::Compute.new(
        :provider => 'Rackspace',
        :version  => 'v2'
      )
    end
  end

end
