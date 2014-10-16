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
require 'resolv'

# top level class for interacting with Google via Fog
class FogProviderGoogle < Provider
  include FogProvider

  # plugin defined resources
  @p12_key_dir = 'api_keys'
  @ssh_key_dir = 'ssh_keys'
  class << self
    attr_accessor :p12_key_dir, :ssh_key_dir
  end

  def create(inputmap)
    @flavor = inputmap['flavor']
    @image = inputmap['image']
    @hostname = inputmap['hostname']
    # Google uses the short hostname as an identifier
    # we keep the server-assigned hostname for use in /etc/hosts
    @providerid = @hostname.split('.').first
    fields = inputmap['fields']
    begin
      # set the provider id in the response
      @result['result']['providerid'] = @providerid

      # set instance variables from our fields
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # validate credentials
      validate!
      # Create the server
      log.debug "Creating #{@providerid} on Google using flavor: #{flavor}, image: #{image}"

      # disks are managed separately, so CREATE must first create and confirm the disk to be used
      # handle boot disk
      create_disk(@providerid, nil, @zone_name, @image)
      disk = confirm_disk(@providerid)

      @disks = [disk]

      # handle additional data disk
      if fields['google_data_disk_size_gb'] && fields['google_data_disk_size_gb'].to_i > 0
        data_disk_name = "#{@providerid}-data"
        log.debug "Creating data disk: #{data_disk_name} of size #{fields['google_data_disk_size_gb']}"
        create_disk(data_disk_name, fields['google_data_disk_size_gb'].to_i, @zone_name, nil)
        data_disk = confirm_disk(data_disk_name)
        @disks.push(data_disk)
      end

      # create the VM
      connection.servers.create(create_server_def)

      # set ssh user
      sshuser =
        if @ssh_user.to_s != ''
          # prefer custom plugin field
          @ssh_user
        elsif @task['config']['sshuser'].to_s != ''
          # default to ssh-user as defined by image
          @task['config']['ssh_user']
        else
          # default to root
          'root'
        end
      @result['result']['ssh-auth']['user'] = sshuser
      @result['result']['ssh-auth']['identityfile'] = File.join(Dir.pwd, self.class.ssh_key_dir, @ssh_key_resource)
      @result['status'] = 0
    # We assume that no work was done when we get Unauthorized
    rescue Excon::Errors::Unauthorized
      @result['status'] = 201
      log.error('Provider credentials invalid/unauthorized')
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderGoogle.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderGoogle.create: #{e.inspect}"
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
      # set instance variables from our fields
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # validate credentials
      validate!
      # Confirm server
      log.debug "Invoking server confirm for id: #{providerid}"
      server = connection.servers.get(providerid)
      # If quota exceeded, the previous create call does not fail, but server will be nil here.
      fail "Unable to retrieve server information for #{providerid}. Please check that you have not reached your quotas" if server.nil?
      # Wait until the server is ready
      fail "Server #{server.name} is in ERROR state" if server.state == 'ERROR'
      log.debug "Waiting for server to come up: #{providerid}"
      server.wait_for(600) { ready? }

      hostname =
        if server.dns_name
          server.dns_name
        elsif server.public_ip_address
          Resolv.getname(server.public_ip_address)
        else
          @task['config']['hostname']
        end

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
      log.debug "Server #{server.name} sshd is up"

      # Process results
      @result['ipaddresses'] = {
        'access_v4' => bootstrap_ip,
        'bind_v4' => bind_ip
      }
      @result['hostname'] = hostname

      # do we need sudo bash?
      sudo = 'sudo' unless @task['config']['ssh-auth']['user'] == 'root'

      set_credentials(@task['config']['ssh-auth'])

      # login with pseudotty and turn off sudo requiretty option
      log.debug "Attempting to ssh to #{bootstrap_ip} as #{@task['config']['ssh-auth']['user']} with credentials: #{@credentials} and pseudotty"
      Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        cmd = "#{sudo} sed -i -e '/^Defaults[[:space:]]*requiretty/ s/^/#/' /etc/sudoers"
        ssh_exec!(ssh, cmd, 'Disabling requiretty via pseudotty session', true)
      end

      # Validate connectivity
      log.debug "Attempting to ssh to #{bootstrap_ip} as #{@task['config']['ssh-auth']['user']} with credentials: #{@credentials}"
      Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 'ping -c1 www.opscode.com', 'Validating external connectivity and DNS resolution via ping')
        ssh_exec!(ssh, "#{sudo} hostname #{hostname}", "Setting hostname to #{hostname}")
      end

      # search for data disk
      server.disks.each do |disk|
        next if disk.key?('boot') && disk['boot'] == true
        # fog attaches additional disks as 'persistent-disk-[index]', google prepends 'google-'
        if disk.key?('deviceName') && disk['deviceName'] =~ /^persistent-disk-(\d+)/
          mount_point = "/data#{Regexp.last_match[1]}"
          mount_point = '/data' if mount_point == '/data1'
          google_disk_id = "google-#{disk['deviceName']}"

          # Mount the data disk
          Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
            # determine mount device
            cmd = "#{sudo} readlink /dev/disk/by-id/#{google_disk_id}"
            device_rel_path = ssh_exec!(ssh, cmd, "Querying disk #{google_disk_id}").first.chomp
            device = File.join('/dev', File.basename(device_rel_path))
            cmd = "#{sudo} mkdir #{mount_point} && #{sudo} /usr/share/google/safe_format_and_mount -m 'mkfs.ext4 -F' #{device} #{mount_point}"
            ssh_exec!(ssh, cmd, "Mounting device #{device} on #{mount_point}")
            # update /etc/fstab
            cmd = "echo '#{device} #{mount_point} ext4 defaults,auto,noatime 0 2' | #{sudo} tee -a /etc/fstab"
            ssh_exec!(ssh, cmd, "Updating fstab for device #{device} on #{mount_point}")
          end
        else
          log.warn "unexpected disk device found, ignoring: #{disk}"
        end
      end

      # disable SELinux
      Net::SSH.start(bootstrap_ip, @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        cmd = "if test -x /usr/sbin/sestatus ; then #{sudo} /usr/sbin/sestatus | grep disabled || ( test -x /usr/sbin/setenforce && #{sudo} /usr/sbin/setenforce Permissive ) ; fi"
        ssh_exec!(ssh, cmd, 'Disabling SELinux')
      end

      @result['status'] = 0
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
      @result['stderr'] = 'Timed out waiting for server to be created'
    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{bootstrap_ip}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{bootstrap_ip}: #{e.inspect}"
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderGoogle.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderGoogle.confirm: #{e.inspect}"
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
      # set instance variables from our fields
      fields.each do |k, v|
        instance_variable_set('@' + k, v)
      end
      # validate credentials
      validate!
      # delete server
      log.debug 'Invoking server delete'

      # check for any disks persisted from a previous delete attempt
      known_disks = @task['config']['disks']

      # fetch server object
      server = connection.servers.get(providerid)

      if known_disks.nil? && !server.nil?
        # this is the first delete attempt, persist the names of the currently attached disks
        known_disks = server.disks.map { |d| d['source'].split('/').last }
        @result['result']['disks'] = known_disks
      end

      # delete server, if it exists
      unless server.nil?
        begin
          server.destroy
          server.wait_for(120) { !ready? }
        rescue Fog::Errors::NotFound
          # ok, can be thrown by wait_for
          log.debug 'Server no longer found'
        end
      end

      # delete any disks
      unless known_disks.nil?
        # query our known_disks to see if they exist
        existing_disks = known_disks.map { |d| connection.disks.get(d) }.compact
        log.debug "existing disks to delete: #{ existing_disks.map(&:name) }"

        # issue destroy to all attached disks
        existing_disks.each do |disk|
          log.debug "Issuing delete for disk #{disk.name}"
          begin
            disk.destroy
          rescue Fog::Errors::NotFound
            log.debug 'Disk already deleted'
          end
        end

        # confirm all disks deleted
        existing_disks.each do |disk|
          begin
            disk.wait_for(120) { !ready? }
          rescue Fog::Errors::NotFound
            log.debug "Disk #{disk.name} no longer found"
          end
          log.debug "Disk #{disk.name} deleted"
        end
      end

      # the server and all known disks have been deleted
      @result['status'] = 0
    rescue Fog::Errors::Error => e
      log.error('Unable to delete specified components: ' + e.inspect)
      @result['stderr'] = "Unable to delete specified components: ' + e.inspect"
    rescue => e
      log.error('Unexpected Error Occurred in FogProviderGoogle.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occurred in FogProviderGoogle.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def connection
    # Create connection
    # rubocop:disable UselessAssignment
    p12_key = File.join(self.class.p12_key_dir, @api_key_resource)
    @connection ||= begin
      connection = Fog::Compute.new(
        provider: 'google',
        google_project: @google_project,
        google_client_email: @google_client_email,
        google_key_location: p12_key
      )
    end
    # rubocop:enable UselessAssignment
  end

  def create_server_def
    server_def = {
      name: @providerid,
      disks: @disks,
      machine_type: @flavor,
      zone_name: @zone_name,
      tags: ['coopr']
    }
    # optional attrs
    server_def[:network] = @network unless @network.to_s == ''
    server_def
  end

  def create_disk(name, size_gb = 10, zone_name, source_image)
    args = {}
    args[:name] = name
    args[:size_gb] = size_gb
    args[:zone_name] = zone_name
    args[:source_image] = source_image unless source_image.nil?

    # check if a disks already exists (retry scenario)
    disk = connection.disks.get(name)
    unless disk.nil?
      # disk of requested name exists already
      existing_size_gb = disk.size_gb
      existing_zone_name = disk.zone_name.nil? ? nil : disk.zone_name.split('/').last
      existing_source_image = disk.source_image.nil? ? nil : disk.source_image.split('/').last
      if size_gb == existing_size_gb && zone_name == existing_zone_name && source_image == existing_source_image
        log.debug "Using pre-exising disk for #{name}, it must not be attached already"
        return disk.name
      else
        fail "Disk #{disk.name} already exists with different specifications"
      end
    end
    disk = connection.disks.create(args)
    disk.name
  end

  def confirm_disk(name)
    disk = connection.disks.get(name)
    disk.wait_for(120) { disk.ready? }
    disk.reload
    disk
  end

  def validate!
    errors = []
    unless @google_client_email =~ /.*gserviceaccount.com$/
      errors << 'Invalid service account email address. It must be in the gserviceaccount.com domain'
    end
    ssh_key = File.join(self.class.ssh_key_dir, @ssh_key_resource)
    p12_key = File.join(self.class.p12_key_dir, @api_key_resource)
    [ssh_key, p12_key].each do |key|
      next if File.readable?(key)
      errors << "Cannot read named key from resource directory: #{key}. Please ensure you have uploaded a key via the UI or API"
    end
    fail 'Credential validation failed!' if errors.each { |e| log.error(e) }.any?
  end
end
