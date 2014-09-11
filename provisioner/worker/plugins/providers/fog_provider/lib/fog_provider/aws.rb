#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
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

  # plugin defined resources
  @@ssh_key_dir = 'ssh_keys'

  def create(inputmap)
    @flavor = inputmap['flavor']
    @image = inputmap['image']
    @hostname = inputmap['hostname']
    fields = inputmap['fields']
    begin
      # Our fields are fog symbols
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Update some variables
      @security_groups = @security_groups.split(',') if @security_groups
      @security_group_ids = @security_group_ids.split(',') if @security_group_ids
      # Run EC2 credential validation
      validate!
      # Create the server
      log.debug "Creating #{hostname} on AWS using flavor: #{flavor}, image: #{image}"
      log.debug 'Invoking server create'
      server = connection.servers.create(create_server_def)
      # Process results
      @result['result']['providerid'] = server.id.to_s
      @result['result']['ssh-auth']['user'] = @task['config']['sshuser'] || 'root'
      @result['result']['ssh-auth']['identityfile'] = File.join(@@ssh_key_dir, @ssh_key_resource) unless @ssh_key_resource.nil?
      @result['status'] = 0
    rescue => e
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
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Run EC2 credential validation
      validate!
      # Confirm server
      log.debug "Invoking server confirm for id: #{providerid}"
      server = connection.servers.get(providerid)
      # Wait until the server is ready
      fail "Server #{server.id} is in ERROR state" if server.state == 'ERROR'
      log.debug "Waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      hostname =
        if !server.dns_name.nil?
          server.dns_name
        elsif !server.public_ip_address.nil?
          Resolv.getname(server.public_ip_address)
        else
          @task['config']['hostname']
        end

      # Handle tags
      hashed_tags = {}
      @tags.map { |t| key, val = t.split('='); hashed_tags[key] = val } unless @tags.nil?
      # Always set the Name tag, so we display correctly in AWS console UI
      unless hashed_tags.keys.include?('Name')
        hashed_tags['Name'] = hostname
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
        fail 'No IP address available for bootstrapping.'
      else
        log.debug "Bootstrap IP address #{bootstrap_ip}"
      end
      bind_ip = server.private_ip_address

      wait_for_sshd(bootstrap_ip, 22)
      log.debug "Server #{server.id} sshd is up"

      # Process results
      @result['ipaddresses'] = {
        'access_v4' => bootstrap_ip,
        'bind_v4' => bind_ip
      }
      @result['hostname'] = hostname
      # do we need sudo bash?
      sudo = 'sudo' unless @task['config']['ssh-auth']['user'] == 'root'
      set_credentials(@task['config']['ssh-auth'])
      # Validate connectivity

      Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        # Backwards-compatibility... ssh_exec! takes 2 arguments prior to 0.9.8
        ssho = method(:ssh_exec!)
        if ssho.arity == 2
          log.debug 'Validating external connectivity and DNS resolution via ping'
          ssh_exec!(ssh, 'ping -c1 www.opscode.com')
          log.debug "Setting hostname to #{hostname}"
          ssh_exec!(ssh, "#{sudo} hostname #{hostname}")
        else
          ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
          ssh_exec!(ssh, "#{sudo} hostname #{hostname}", "Setting hostname to #{hostname}")
          # Setting up disks
          begin
            # m1.small uses /dev/xvda2 for data
            xvda = true
            xvdb = false
            xvdc = false
            ssh_exec!(ssh, 'test -e /dev/xvda2 && echo yes', 'Checking for /dev/xvda2')
          rescue
            xvda = false
            begin
              xvdb = true
              ssh_exec!(ssh, 'test -e /dev/xvdb && echo yes', 'Checking for /dev/xvdb')
              # Do we have /dev/xvdc, too?
              begin
                xvdc = true
                ssh_exec!(ssh, 'test -e /dev/xvdc && echo yes', 'Checking for /dev/xvdc')
              rescue
                xvdc = false
              end
            rescue
              xvdb = false
            end
          end

          log.debug 'Found the following:'
          log.debug "- xvda = #{xvda}"
          log.debug "- xvdb = #{xvdb}"
          log.debug "- xvdc = #{xvdc}"

          # Now, do the right thing
          if xvdc
            # Check for APT
            begin
              apt = true
              ssh_exec!(ssh, 'which apt-get', 'Checking for apt-get')
            rescue
              apt = false
            end
            # Install mdadm
            if apt
              ssh_exec!(ssh, "#{sudo} apt-get update", 'Running apt-get update')
              # Setup nullmailer
              ssh_exec!(ssh, "echo 'nullmailer shared/mailname string localhost' | #{sudo} debconf-set-selections && echo 'nullmailer nullmailer/relayhost string localhost' | #{sudo} debconf-set-selections", 'Configuring nullmailer')
              ssh_exec!(ssh, "#{sudo} apt-get install nullmailer -y", 'Installing nullmailer')
              ssh_exec!(ssh, "#{sudo} apt-get install mdadm -y", 'Installing mdadm')
            else
              ssh_exec!(ssh, "#{sudo} yum install mdadm -y", 'Installing mdadm')
            end
            ssh_exec!(ssh, "mount | grep ^/dev/xvdb 2>&1 >/dev/null && #{sudo} umount /dev/xvdb || true", 'Unmounting /dev/xvdb')
            # Setup RAID
            log.debug 'Setting up RAID0'
            ssh_exec!(ssh, "echo yes | #{sudo} mdadm --create /dev/md0 --level=0 --raid-devices=$(ls -1 /dev/xvd[b-z] | wc -l) $(ls -1 /dev/xvd[b-z])", 'Creating /dev/md0 RAID0 array')
            if apt
              ssh_exec!(ssh, "#{sudo} su - -c 'mdadm --detail --scan >> /etc/mdadm/mdadm.conf'", 'Write /etc/mdadm/mdadm.conf')
            else
              ssh_exec!(ssh, "#{sudo} su - -c 'mdadm --detail --scan >> /etc/mdadm.conf'", 'Write /etc/mdadm.conf')
            end
            ssh_exec!(ssh, "#{sudo} sed -i -e 's:xvdb:md0:' /etc/fstab", 'Update /etc/fstab for md0')
            ssh_exec!(ssh, "#{sudo} /sbin/mkfs.ext4 /dev/md0 && #{sudo} mkdir -p /data && #{sudo} mount -o _netdev /dev/md0 /data", 'Mounting /dev/md0 as /data')
          elsif xvdb
            ssh_exec!(ssh, "mount | grep ^/dev/xvdb 2>&1 >/dev/null && #{sudo} umount /dev/xvdb && #{sudo} /sbin/mkfs.ext4 /dev/xvdb && #{sudo} mkdir -p /data && #{sudo} mount -o _netdev /dev/xvdb /data", 'Mounting /dev/xvdb as /data')
          else
            ssh_exec!(ssh, "mount | grep ^/dev/xvda2 2>&1 >/dev/null && #{sudo} umount /dev/xvda2 && #{sudo} /sbin/mkfs.ext4 /dev/xvda2 && #{sudo} mkdir -p /data && #{sudo} mount -o _netdev /dev/xvda2 /data", 'Mounting /dev/xvda2 as /data')
          end
          ssh_exec!(ssh, "#{sudo} sed -i -e 's:/mnt:/data:' /etc/fstab", 'Updating /etc/fstab for /data')
        end
      end
      # Return 0
      @result['status'] = 0
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
      @result['stderr'] = 'Timed out waiting for server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{bootstrap_ip}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{bootstrap_ip}: #{e.inspect}"
    rescue => e
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
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # Run EC2 credential validation
      validate!
      # Delete server
      log.debug 'Invoking server delete'
      begin
        server = connection.servers.get(providerid)
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
    # Create connection
    # rubocop:disable UselessAssignment
    @connection ||= begin
      connection = Fog::Compute.new(
        provider: 'AWS',
        aws_access_key_id: @api_user,
        aws_secret_access_key: @api_password,
        region: @aws_region
      )
    end
    # rubocop:enable UselessAssignment
  end

  def iam_name_from_profile(profile)
    # The IAM profile object only contains the name as part of the arn
    if profile && profile.key?('arn')
      name = profile['arn'].split('/')[-1]
    end
    name || ''
  end

  def validate!(keys = [@api_user, @api_password])
    errors = []
    # Check for credential file and load it
    unless @aws_credential_file.nil?
      unless (keys & [@api_user, @api_password]).empty?
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
      pretty_key = k.to_s.gsub(/_/, ' ').gsub(/\w+/) { |w| (w =~ /(ssh)|(aws)/i) ? w.upcase  : w.capitalize }
      errors << "You did not provide a valid '#{pretty_key}' value." if k.nil?
    end
    # Check for errors
    fail 'Credential validation failed!' if errors.each { |e| log.error(e) }.any?
  end

  def vpc_mode?
    # Amazon Virtual Private Cloud requires a subnet_id
    !@subnet_id.nil?
  end

  def ami
    @ami ||= connection.images.get(@image)
  end

  def tags
    tags = @tags
    if !tags.nil? && tags.length != tags.to_s.count('=')
      log.error 'Tags should be entered in a key=value pair'
      fail 'Tags should be entered in a key=value pair'
    end
    tags
  end

  def create_server_def
    server_def = {
      flavor_id: @flavor,
      image_id: @image,
      groups: @security_groups,
      security_group_ids: @security_group_ids,
      key_name: @ssh_keypair,
      availability_zone: @availability_zone,
      placement_group: @placement_group,
      iam_instance_profile_name: @iam_instance_profile
    }
    server_def[:subnet_id] = @subnet_id if vpc_mode?
    server_def[:tenancy] = 'dedicated' if vpc_mode? && @dedicated_instance
    server_def[:associate_public_ip] = 'true' if vpc_mode? && @associate_public_ip
    server_def
  end

  def create_tags(hashed_tags, providerid)
    hashed_tags.each_pair do |key, val|
      connection.tags.create key: key, value: val, resource_id: providerid
    end
  end
end
