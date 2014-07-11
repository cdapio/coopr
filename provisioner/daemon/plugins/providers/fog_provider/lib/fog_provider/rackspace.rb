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
      instance_result = instance.run

      # Process results
      @result['result']['providerid'] = instance_result['providerid']
      @result['result']['ssh-auth']['user'] = 'root'
      if instance_result.key?('rootpassword')
        @result['result']['ssh-auth']['password'] = instance_result['rootpassword']
      end
      @result['status'] = instance_result['status']

    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderRackspace.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderRackspace.create: #{e.inspect}"
    else
      log.debug "Create finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
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

  # Shared definitions (borrowed from knife-rackspace gem, Apache 2.0 license)

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

  def ip_address(server, network = 'public')
    if network == 'public' && v2_access_ip(server) != ''
      v2_access_ip(server)
    else
      v2_ip_address(server, network)
    end
  end

  def v2_ip_address(server, network)
    network_ips = server.addresses[network]
    extract_ipv4_address(network_ips) if network_ips
  end

  def v2_access_ip(server)
    server.access_ipv4_address.nil? ? '' : server.access_ipv4_address
  end

  def extract_ipv4_address(ip_addresses)
    address = ip_addresses.select { |ip| ip['version'] == 4 }.first
    address ? address['addr'] : ''
  end

end
