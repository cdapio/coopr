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
        instance_variable_set('@' + k, v)
      end
      # Create the server
      log.debug 'Invoking server create'
      begin
        server = connection.servers.create(
          :flavor_id    => flavor,
          :image_id     => image,
          :name         => hostname,
          :config_drive => false
        )
        # :keypair      => @rackspace_ssh_keypair if @rackspace_ssh_keypair
        server.save
      end
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = 'root'
      @result['result']['ssh-auth']['password'] = server.password unless server.password.nil?
      @result['status'] = 0
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
        instance_variable_set('@' + k, v)
      end
      # Confirm server
      log.debug 'Invoking server confirm'

      log.debug "fetching server for id: #{providerid}"
      server = self.connection.servers.get(providerid)

      # Wait until the server is ready
      begin
        log.debug "waiting for server to come up: #{providerid}"
        server.wait_for(600) { ready? }
      rescue Fog::Errors::TimeoutError
        log.error 'Timeout waiting for the server to be created'
      end

      @bootstrap_ip = ip_address(server, 'public')
      if @bootstrap_ip.nil?
        log.error 'No IP address available for bootstrapping.'
        exit 1
      else
        log.debug "Bootstrap IP address #{@bootstrap_ip}"
      end

      wait_for_sshd
      log.info "Server #{server.name} sshd is up"

      # Process results
      @result['result']['ipaddress'] = @bootstrap_ip
      # Additional checks
      log.debug 'Confirming sshd is up'
      tcp_test_port(@bootstrap_ip, 22) { sleep 5 }
      set_credentials(@task['config']['ssh-auth'])
      # Validate connectivity
      Net::SSH.start(@result['result']['ipaddress'], @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
      end
      # Return 0
      @result['status'] = 0
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}: #{e.inspect}"
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
        instance_variable_set('@' + k, v)
      end
      # Delete server
      log.debug 'Invoking server delete'
      begin
        server = self.connection.servers.get(providerid)
        server.destroy
      rescue NoMethodError
        log.warn "Could not locate server '#{providerid}'... skipping"
      end
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
    log.debug "rackspace_api_key #{@rackspace_api_key}"
    log.debug "rackspace_username #{@rackspace_username}"
    log.debug "rackspace_region #{@rackspace_region}"

    # Create connection
    @connection ||= begin
      connection = Fog::Compute.new(
        :provider => 'Rackspace',
        :version  => 'v2',
        :rackspace_username => @rackspace_username,
        :rackspace_api_key  => @rackspace_api_key,
        :rackspace_region   => @rackspace_region
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
