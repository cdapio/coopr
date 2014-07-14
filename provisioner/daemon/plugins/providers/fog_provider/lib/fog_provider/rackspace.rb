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
require_relative 'rackspace/confirm'
require_relative 'rackspace/delete'
require_relative '../fog_provider'

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
    begin
      # Our fields are fog symbols
      fields.each do |k,v|
        k.to_sym = v
      end
      # Confirm server
      log.debug 'Invoking server confirm'
      instance = FogProviderRackspaceConfirm.new
      instance_result = instance.run
      # Process results
      @result['result']['ipaddress'] = instance_result['ipaddress']
      raise "non-zero exit code: #{instance_result['ipaddress']} from FogProviderRackspaceConfirm" unless instance_result['status'] == 0
      # Additional checks
      log.debug 'Confirming sshd is up'
      instance.tcp_test_port(instance_result['ipaddress'], 22) { sleep 5 }
      set_credentials(@task['config']['ssh-auth'])
      # Validate connectivity
      Net::SSH.start(instance_result['ipaddress'], @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
      end
      # Return 0
      @result['status'] = 0
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{instance_result['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{instance_result['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderRackspace.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderRackspace.confirm: #{e.inspect}"
    else
      log.debug "Confirm finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def delete(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k,v|
        k.to_sym = v
      end
      # Delete server
      log.debug 'Invoking server delete'
      instance = FogProviderRackspaceDelete.new
      instance_result = instance.run
      # Return 0
      @result['status'] = 0
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderRackspace.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderRackspace.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
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
