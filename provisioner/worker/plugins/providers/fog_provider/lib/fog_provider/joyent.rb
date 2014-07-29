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

class FogProviderJoyent < FogProvider

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
      log.info "Creating #{hostname} on Joyent using flavor: #{flavor}, image: #{image}"
      log.debug 'Invoking server create'
      begin
        server = connection.servers.create(
          :package         => flavor,
          :dataset         => image,
          :name            => hostname,
          :key_name        => @joyent_keyname
        )
      end
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = 'root'
      @result['result']['ssh-auth']['password'] = @joyent_password unless @joyent_password.nil?
      @result['result']['ssh-auth']['identityfile'] = @joyent_keyfile unless @joyent_keyfile.nil?
      @result['status'] = 0
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderJoyent.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderJoyent.create: #{e.inspect}"
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
      raise 'Server #{server.name} is in ERROR state' if server.state == 'ERROR'
      log.debug "waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      @bootstrap_ip = ip_address(server)
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
        ssh_exec!(ssh, "hostname #{@task['config']['hostname']}", 'Temporarily setting hostname')
      end
      # Return 0
      @result['status'] = 0
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occured in FogProviderJoyent.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderJoyent.confirm: #{e.inspect}"
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
      log.error('Unexpected Error Occured in FogProviderJoyent.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in FogProviderJoyent.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # Shared definitions (borrowed from knife-joyent gem, Apache 2.0 license)

  def connection
    log.debug "Connection options for Joyent:"
    log.debug "- joyent_username #{@joyent_username}"
    log.debug "- joyent_password #{@joyent_password}"
    log.debug "- joyent_keyname #{@joyent_keyname}"
    log.debug "- joyent_keyfile #{@joyent_keyfile}"
    log.debug "- joyent_api_url #{@joyent_api_url}"
    log.debug "- joyent_version #{@joyent_version}"

    # Create connection
    @connection ||= begin
      connection = Fog::Compute.new(
        :provider => 'Joyent',
        :joyent_username => @joyent_username,
        :joyent_password => @joyent_password,
        :joyent_keyname  => @joyent_keyname,
        :joyent_keyfile  => @joyent_keyfile,
        :joyent_url      => @joyent_api_url,
        :joyent_version  => @joyent_version
      )
    end
  end

  def ip_address(server)
    server_ips = server.ips.select{ |ip| ip && !(is_loopback(ip) || is_linklocal(ip)) }
    if server_ips.count === 1
      server_ips.first
    else
      server_ips.find{ |ip| !is_private(ip) }
    end
  end

end
