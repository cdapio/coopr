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

class FogProviderOpenstack < FogProvider

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
      log.info "Creating #{hostname} on Openstack using flavor: #{flavor}, image: #{image}"
      log.debug 'Invoking server create'
      begin
        server = connection.servers.create(
          :flavor_ref      => flavor,
          :image_ref       => image,
          :name            => hostname,
          :security_groups => @security_groups,
          :key_name        => @openstack_ssh_keypair
        )
      end
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = 'root'
      @result['result']['ssh-auth']['password'] = server.password unless server.password.nil?
      @result['result']['ssh-auth']['identityfile'] = @openstack_identity_file if @openstack_identity_file
      @result['status'] = 0
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderOpenstack.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderOpenstack.create: #{e.inspect}"
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
      log.debug "waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      @bootstrap_ip = ip_address(server, 'public')
      if @bootstrap_ip.nil?
        log.error 'No IP address available for bootstrapping.'
      else
        log.debug "Bootstrap IP address #{@bootstrap_ip}"
      end

      wait_for_sshd(@bootstrap_ip, 22)
      log.debug "Server #{server.name} sshd is up"

      # Process results
      @result['result']['ipaddress'] = @bootstrap_ip
      # Additional checks
      set_credentials(@task['config']['ssh-auth'])
      # Validate connectivity
      Net::SSH.start(@result['result']['ipaddress'], @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
      end
      # Return 0
      @result['status'] = 0
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderOpenstack.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderOpenstack.confirm: #{e.inspect}"
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
      log.error('Unexpected Error Occured in FogProviderOpenstack.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderOpenstack.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # Shared definitions (borrowed from knife-openstack gem, Apache 2.0 license)

  def connection
    log.debug "Connection options for Openstack:"
    log.debug "- openstack_username #{@openstack_username}"
    log.debug "- openstack_password #{@openstack_password}"
    log.debug "- openstack_tenant #{@openstack_tenant}"
    log.debug "- openstack_auth_url #{@openstack_auth_url}"
    log.debug "- openstack_ssl_verify_peer #{@openstack_ssl_verify_peer}"

    # Create connection
    @connection ||= begin
      connection = Fog::Compute.new(
        :provider => 'OpenStack',
        :openstack_auth_url => @openstack_auth_url,
        :openstack_username => @openstack_username,
        :openstack_tenant   => @openstack_tenant,
        :openstack_api_key  => @openstack_password,
        :connection_options => {
          :ssl_verify_peer => @openstack_ssl_verify_peer
        }
      )
    end
  end

  def ip_address(server, network = 'public')
    network_ips = server.addresses[network]
    extract_ipv4_address(network_ips) if network_ips
  end

  def extract_ipv4_address(ip_addresses)
    address = ip_addresses.select { |ip| ip['version'] == 4 }.first
    address ? address['addr'] : ''
  end

end
