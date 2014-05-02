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

require 'json'
require 'net/scp'

class ChefAutomator < Automator
  attr_accessor :credentials, :cookbooks_path, :cookbooks_tar, :remote_cache_dir

  def initialize(task)
    super(task)
    @chef_primitives_path = "#{File.expand_path(File.dirname(__FILE__))}/chef_automator"
    @remote_cache_dir = "/var/cache/loom"
    @remote_chef_dir = "/var/chef"
  end

  # create local tarballs of the cookbooks, roles, data_bags, etc to be scp'd to remote machine
  def generate_chef_primitive_tar(chef_primitive)

    chef_primitive_path = "#{@chef_primitives_path}/#{chef_primitive}"
    chef_primitive_tar = "#{@chef_primitives_path}/#{chef_primitive}.tar.gz"

    # limit tarball regeneration to once per 10min
    if !File.exists?(chef_primitive_tar) or ((Time.now - File.stat(chef_primitive_tar).mtime).to_i > 600)
      log.debug "Generating #{chef_primitive_tar} from #{chef_primitive_path}"
      `tar -czf "#{chef_primitive_tar}.new" -C "#{@chef_primitives_path}" #{chef_primitive}`
      `mv "#{chef_primitive_tar}.new" "#{chef_primitive_tar}"`
      log.debug "Generation complete: #{chef_primitive_tar}"
    end
  end


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

  # generate the chef run json_attributes from the loom task metadata
  def generate_chef_json_attributes(servicestring)

    servicedata = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }

    if (servicestring.nil? || servicestring == "")
      servicestring = "{}"
    end
    # service data is passed here as an escaped json string
    servicedata.merge!(JSON.parse(servicestring))
    log.debug "Tasks before merging: #{@task}"

    # cluster and nodes data is passed as expanded hash
    clusterdata = @task['config']['cluster']
    if (clusterdata.nil? || clusterdata == "")
      clusterdata = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    end
    nodesdata = @task['config']['nodes']
    if (nodesdata.nil? || nodesdata == "")
      nodesdata = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    end

    # merge data together into expected layout for json_attributes
    clusterdata['nodes'] = nodesdata
    servicedata['loom']['cluster'] = clusterdata

    # we also need to merge cluster config top-level
    servicedata.merge!(clusterdata)

    # generate the json
    loomdatajson = JSON.generate(servicedata)
    log.debug "Generated JSON attributes: #{loomdatajson}"
    
    loomdatajson
  end

  # bootstrap remote machine: install chef, copy all cookbooks, data_bags, etc in to place
  def bootstrap(inputmap)
    sshauth = inputmap['sshauth']
    hostname = inputmap['hostname']
    ipaddress = inputmap['ipaddress']

    set_credentials(sshauth)

    %w[cookbooks data_bags roles].each do |chef_primitive|
      generate_chef_primitive_tar(chef_primitive)
    end

    log.debug "Attempting ssh into ip: #{@task["config"]["ipaddress"]}, user: #{@task["config"]["ssh-auth"]["user"]}"

    begin
      Net::SSH.start(ipaddress, inputmap['sshauth']['user'], @credentials) do |ssh|

        # validate connectivity
        log.debug "Validating connectivity to #{hostname}"
        output = ssh_exec!(ssh, "hostname")

        # determine if curl is installed, else default to wget
        log.debug "Checking for curl"
        chef_install_cmd = "curl -L https://www.opscode.com/chef/install.sh | bash"
        begin
          ssh_exec!(ssh, "which curl")
        rescue
          log.debug "curl not found, defaulting to wget"
          chef_install_cmd = "wget -qO - https://www.opscode.com/chef/install.sh | bash"
        end

        # install chef
        log.debug "Install chef..."
        output = ssh_exec!(ssh, chef_install_cmd)
        if (output[2] != 0 )
          log.error "Chef install failed: #{output}"
          raise "Chef install failed: #{output}"
        end
        log.debug "Chef installed successfully..."

       # confirm chef installation
        output = ssh_exec!(ssh, "type chef-solo")
        if (output[2] != 0 )
          log.error "Chef install validation failed: #{output}"
          raise "Chef install validation failed: #{output}"
        end
        log.debug "Chef install validated successfully..."

        # create @remote_cache_dir
        output = ssh_exec!(ssh, "mkdir -p #{@remote_cache_dir}")
        if (output[2] != 0 )
          log.error "Unable to create #{@remote_cache_dir} on #{hostname} : #{output}"
          raise "Unable to create #{@remote_cache_dir} on #{hostname} : #{output}"
        end

        # create @remote_chef_dir
        output = ssh_exec!(ssh, "mkdir -p #{@remote_chef_dir}")
        if (output[2] != 0 )
          log.error "Unable to create #{@remote_chef_dir} on #{hostname} : #{output}"
          raise "Unable to create #{@remote_chef_dir} on #{hostname} : #{output}"
        end

      end
    rescue Net::SSH::AuthenticationFailed => e
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # check to ensure scp is installed and attempt to install it
    begin
      Net::SSH.start(ipaddress, inputmap['sshauth']['user'], @credentials) do |ssh|

        log.debug "Checking for scp installation"
        begin
          ssh_exec!(ssh, "which scp")
        rescue
          log.warn "scp not found, attempting to install openssh-client"
          scp_install_cmd = "yum -qy install openssh-clients"
          begin
            ssh_exec!(ssh, "which yum")
          rescue
            scp_install_cmd = "apt-get -qy install openssh-client"
          end

          begin
            log.debug "installing openssh-client via #{scp_install_cmd}"
            ssh_exec!(ssh, scp_install_cmd)
          rescue => e
            raise $!, "Could not install scp on #{ipaddress}: #{$!}", $!.backtrace
          end
        end
        log.debug "scp found on remote"
      end
    rescue Net::SSH::AuthenticationFailed => e
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # upload tarballs to target machine
    %w[cookbooks data_bags roles].each do |chef_primitive|
      log.debug "Uploading #{chef_primitive} from #{@chef_primitives_path}/#{chef_primitive}.tar.gz to #{ipaddress}:#{@remote_cache_dir}/#{chef_primitive}.tar.gz"
      begin
        Net::SCP.upload!(ipaddress, sshauth["user"], "#{@chef_primitives_path}/#{chef_primitive}.tar.gz", "#{@remote_cache_dir}/#{chef_primitive}.tar.gz", :ssh =>
            @credentials)
      rescue Net::SSH::AuthenticationFailed => e
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
      log.debug "Upload complete"
    end

    # extract tarballs on remote machine to /var/chef
    %w[cookbooks data_bags roles].each do |chef_primitive|
      begin
        Net::SSH.start(ipaddress, inputmap['sshauth']['user'], @credentials) do |ssh|
          output = ssh_exec!(ssh, "tar xf #{@remote_cache_dir}/#{chef_primitive}.tar.gz -C #{@remote_chef_dir}")
          if (output[2] != 0 )
            log.error "Error extracting remote #{@remote_cache_dir}/#{chef_primitive}.tar.gz: #{output}"
            raise "Error extracting remote #{@remote_cache_dir}/#{chef_primitive}.tar.gz: #{output}"
          end
        end
      rescue Net::SSH::AuthenticationFailed => e
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
    end

    @result['status'] = 0

    log.info "ChefAutomator.bootstrap completed successfully: #{@result}"
    @result
  end


  def runchef(inputmap)
    sshauth = inputmap['sshauth']
    hostname = inputmap['hostname']
    ipaddress = inputmap['ipaddress']
    fields = inputmap['fields']

    raise "required parameter \"run_list\" not found in input: #{fields}" if fields['run_list'].nil?
    # run_list as specified by user
    run_list = fields['run_list']
    # whitespace in the runlist is not allowed
    run_list.gsub!(/\s+/, "")

    # additional json attributes defined for this service action
    json_attributes = fields['json_attributes']

    # merge together json_attributes, cluster config, loom node data
    jsondata = generate_chef_json_attributes(json_attributes)

    set_credentials(sshauth)


    begin
      # write json attributes to a local tmp file
      tmpjson = Tempfile.new("loom")
      tmpjson.write(jsondata)
      tmpjson.close

      # scp task.json to remote
      log.debug "Copying json attributes to remote"
      begin
        Net::SCP.upload!(ipaddress, inputmap['sshauth']['user'], tmpjson.path, "#{@remote_cache_dir}/#{@task['taskId']}.json", :ssh =>
          @credentials)
      rescue Net::SSH::AuthenticationFailed => e
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
      log.debug "Copy json attributes complete"

    ensure
      tmpjson.close
      tmpjson.unlink
    end

    begin
      Net::SSH.start(ipaddress, inputmap['sshauth']['user'], @credentials) do |ssh|

        log.debug "Running chef-solo"
        output = ssh_exec!(ssh, "chef-solo -j #{@remote_cache_dir}/#{@task['taskId']}.json -o '#{run_list}'")
        if (output[2] != 0 )
          log.error "Chef run did not complete successfully: #{output}"
          raise "Chef run did not complete successfully: #{output}"
        end
      end
    rescue Net::SSH::AuthenticationFailed => e
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    @result['status'] = 0
    log.info "Chef run completed successfully for task #{@task['taskId']}: #{@result}"
    @result
  end

  def install(inputmap)
    log.info "ChefAutomator performing install task #{@task['taskId']}"
    runchef(inputmap)
  end

  def configure(inputmap)
    log.info "ChefAutomator performing configure task #{@task['taskId']}"
    runchef(inputmap)
  end

  def init(inputmap)
    log.info "ChefAutomator performing initialize task #{@task['taskId']}"
    runchef(inputmap)
  end

  def start(inputmap)
    log.info "ChefAutomator performing start task #{@task['taskId']}"
    runchef(inputmap)
  end

  def stop(inputmap)
    log.info "ChefAutomator performing stop task #{@task['taskId']}"
    runchef(inputmap)
  end

  def remove(inputmap)
    log.info "ChefAutomator performing remove task #{@task['taskId']}"
    runchef(inputmap)
  end

end

