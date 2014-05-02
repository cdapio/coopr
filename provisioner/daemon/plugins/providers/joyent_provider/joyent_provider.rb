#!/usr/bin/env ruby
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

gem 'knife-joyent', '= 0.4.8'

require 'json'
require_relative 'chef/knife/loom_joyent_server_create'
require_relative 'chef/knife/loom_joyent_server_confirm'
require 'chef/knife/joyent_server_delete'
require 'net/ssh'

class JoyentProvider < Provider

  def create(inputmap)
    flavor = inputmap['flavor']
    image = inputmap['image']
    hostname = inputmap['hostname']
    fields = inputmap['fields']

    begin
      knife_instance = Chef::Knife::LoomJoyentServerCreate.new
      knife_instance.configure_chef

      knife_instance.config[:package] = flavor
      knife_instance.config[:dataset] = image
      knife_instance.config[:chef_node_name] = hostname

      # our plugin-defined fields are chef knife configs
      fields.each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Invoking server create"
      kniferesult = knife_instance.run
      @result['result']['providerid'] = kniferesult['providerid']
      @result['result']['ssh-auth']['user'] = "root"
      if fields.key?('joyent_keyfile')
        @result['result']['ssh-auth']['identityfile'] = fields['joyent_keyfile']
      elsif fields.key?('joyent_password')
        @result['result']['ssh-auth']['password'] = fields['joyent_password']
      end
      @result['status'] = kniferesult['status']

    rescue Excon::Errors::Conflict => e
      if e.response && e.response.body.kind_of?(String)
        error = ::Fog::JSON.decode(e.response.body)
        log.error(error['message'])
        if error.key?('errors') && error['errors'].kind_of?(Array)
          error['errors'].each do |err|
            log.error " * [#{err['field']}] #{err['message']}"
          end
        end
        @result['stderr'] = error.inspect
      else
        log.error e.message
        @result['stderr'] = e.message
      end

    rescue Exception => e
      log.error('Unexpected Error Occured in JoyentProvider.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured: #{e.inspect}"
    else
      log.info "Create finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def confirm(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']

    begin
      knife_instance = Chef::Knife::LoomJoyentServerConfirm.new
      knife_instance.configure_chef
      knife_instance.name_args.push(providerid)

      # our plugin-defined fields are chef knife configs
      fields.each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Waiting to confirm creation of server"
      kniferesult = knife_instance.run

      @result['result']['ipaddress'] = kniferesult['ipaddress']
      raise "non-zero exit code: #{kniferesult['status']} from knife rackspace" unless kniferesult['status'] == 0

      # additional checks, we want to make sure we can login and verify external dns
      log.debug "confirming ssh is up"
      knife_instance.tcp_test_ssh(kniferesult['ipaddress'])
      set_credentials(@task['config']['ssh-auth'])

      Net::SSH.start(kniferesult['ipaddress'], @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        # validate connectivity
        log.debug "Validating dns resolution/connectivity"
        output = ssh_exec!(ssh, "ping -c1 www.opscode.com")

        log.debug "Temporarily setting hostname.  this will not surive a reboot!"
        output = ssh_exec!(ssh, "hostname #{@task['config']['hostname']}")
      end

      @result['status'] = 0
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{kniferesult['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{kniferesult['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occured in JoyentPlugin.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in JoyentPlugin.confirm: #{e.inspect}"
    else
      log.info "Confirmed that server was created successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def delete(inputmap)
    providerid = inputmap['providerid']
    fields = inputmap['fields']

    begin
      knife_instance = Chef::Knife::JoyentServerDelete.new
      knife_instance.configure_chef
      knife_instance.name_args.push(@task["config"]["providerid"])
      knife_instance.config[:yes] = true

      # our plugin-defined fields are chef knife configs
      fields.each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Invoking server delete"
      kniferesult = knife_instance.run
      @result['status'] = 0
    # joyent plugin always attempts to delete from chef server
    # handle no chef server configured
    rescue Chef::Exceptions::PrivateKeyMissing => e
      @result['status'] = 0
    # handle chef server configured but node not found
    rescue Net::HTTPServerException => e 
      if e.response.code == "404"
        @result['status'] = 0
      else
        raise
      end
    rescue Exception => e
      log.error('Unexpected Error Occured in JoyentPlugin.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in JoyentPlugin.delete: #{e.inspect}"
    else 
      log.info "Delete finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # used by ssh validation in confirm stage
  def set_credentials(sshauth)
    @credentials = Hash.new
    @credentials[:paranoid] = false
    sshauth.each do |k, v|
      if (k =~ /password/)
        @credentials[:password] = v
      elsif (k =~ /identityfile/)
        @credentials[:keys] = [ v ]
      end
    end
  end

end

