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

require_relative 'utils'
require 'readline'
require 'resolv'

class FogProviderAWS < Provider

  include FogProvider

  def create(inputmap)
    @flavor = inputmap['flavor']
    @image = inputmap['image']
    @hostname = inputmap['hostname']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k,v|
        instance_variable_set('@' + k, v)
      end
      # Update some variables
      @security_groups = @security_groups.split(',') if @security_groups
      @security_group_ids = @security_group_ids.split(',') if @security_group_ids
      # Run EC2 credential validation
      validate!
      # Create the server
      log.info "Creating #{hostname} on AWS using flavor: #{flavor}, image: #{image}"
      log.debug 'Invoking server create'
      server = connection.servers.create(create_server_def)
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = @task['config']['ssh-auth']['user'] || 'root'
      @result['result']['ssh-auth']['identityfile'] = @aws_keyfile unless @aws_keyfile.nil?
      @result['status'] = 0
    rescue Exception => e
      log.error('Unexpected Error Occurred in FogProviderAWS.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderAWS.create: #{e.inspect}"
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
      # Run EC2 credential validation
      validate!
      # Confirm server
      log.debug "Invoking server confirm for id: #{providerid}"
      server = self.connection.servers.get(providerid)
      # Wait until the server is ready
      raise 'Server #{server.id} is in ERROR state' if server.state == 'ERROR'
      log.debug "waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      # Handle tags
      hashed_tags = {}
      @tags.map{ |t| key,val=t.split('='); hashed_tags[key]=val} unless @tags.nil?
      # Always set the Name tag, so we display correctly in AWS console UI
      unless hashed_tags.keys.include?('Name')
        hashed_tags['Name'] = @task['config']['hostname']
      end
      create_tags(hashed_tags, providerid) unless hashed_tags.empty?

      bootstrap_ip =
        if server.public_ip_address
          server.public_ip_address
        else
          Resolv.getaddress(server.dns_name) unless server.dns_name.nil?
        end
      if bootstrap_ip.nil?
        log.error 'No IP address available for bootstrapping.'
      else
        log.debug "Bootstrap IP address #{bootstrap_ip}"
      end
      bind_ip = server.private_ip_address

      sleep 30
      wait_for_sshd(bootstrap_ip, 22)
      log.debug "Server #{server.id} sshd is up"

      # Process results
      @result['result']['ipaddress'] = bootstrap_ip
      @result['result']['ipaddresses'] = {
        'access_v4' => bootstrap_ip,
        'bind_v4' => bind_ip
      }
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
      @result['stderr'] = 'Timed out waiting for server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{@result['result']['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occurred in FogProviderAWS.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderAWS.confirm: #{e.inspect}"
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
      # Run EC2 credential validation
      validate!
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
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderAWS.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderAWS.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # Shared definitions (borrowed from knife-ec2 gem, Apache 2.0 license)

  def connection
    log.debug "Connection options for AWS:"
    log.debug "- aws_access_key_id #{@aws_access_key}"
    log.debug "- aws_secret_access_key #{@aws_secret_key}"
    log.debug "- aws_region #{@aws_region}"

    # Create connection
    @connection ||= begin
      connection = Fog::Compute.new(
        :provider => 'AWS',
        :aws_access_key_id     => @aws_access_key,
        :aws_secret_access_key => @aws_secret_key,
        :region                => @aws_region
      )
    end
  end

  def iam_name_from_profile(profile)
    # The IAM profile object only contains the name as part of the arn
    if profile && profile.key?('arn')
      name = profile['arn'].split('/')[-1]
    end
    name ||= ''
  end

  def validate!(keys=[@aws_access_key, @aws_secret_key])
    errors = []
    # Check for credential file and load it
    unless @aws_credential_file.nil?
      unless (keys & [@aws_access_key, @aws_secret_key]).empty?
        errors << 'Either provide a credentials file or the access key and secret keys but not both.'
      end
      # File format:
      # AWSAccessKeyId=somethingsomethingdarkside
      # AWSSecretKey=somethingsomethingcomplete
      entries = Hash[*File.read(@aws_credential_file).split(/[=\n]/).map(&:chomp)]
      @aws_access_key_id = entries['AWSAccessKeyId']
      @aws_secret_access_key = entries['AWSSecretKey']
    end
    # Validate keys
    keys.each do |k|
      pretty_key = k.to_s.gsub(/_/, ' ').gsub(/\w+/){ |w| (w =~ /(ssh)|(aws)/i) ? w.upcase  : w.capitalize }
      if k.nil?
        errors << "You did not provide a valid '#{pretty_key}' value."
      end
    end
    # Check for errors
    if errors.each{|e| log.error(e)}.any?
      raise 'Credential validation failed!'
    end
  end

  def vpc_mode?
    # Amazon Virtual Private Cloud requires a subnet_id
    !!@subnet_id
  end

  def ami
    @ami ||= connection.images.get(@image)
  end

  def tags
    tags = @tags
    if !tags.nil? && tags.length != tags.to_s.count('=')
      log.error 'Tags should be entered in a key=value pair'
      raise 'Tags should be entered in a key=value pair'
    end
    tags
  end

  def create_server_def
    server_def = {
      :flavor_id                 => @flavor,
      :image_id                  => @image,
      :groups                    => @security_groups,
      :security_group_ids        => @security_group_ids,
      :key_name                  => @aws_keyname,
      :availability_zone         => @availability_zone,
      :placement_group           => @placement_group,
      :iam_instance_profile_name => @iam_instance_profile
    }
    server_def[:subnet_id] = @subnet_id if vpc_mode?
    server_def[:tenancy] = 'dedicated' if vpc_mode? && @dedicated_instance
    server_def[:associate_public_ip] = !!@associate_public_ip if vpc_mode? && @associate_public_ip
    server_def
  end

  def create_tags(hashed_tags, providerid)
    hashed_tags.each_pair do |key,val|
      connection.tags.create :key => key, :value => val, :resource_id => providerid
    end
  end

end
