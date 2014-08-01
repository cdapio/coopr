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

require 'json'
require 'net/scp'

class ChefSoloAutomator < Automator
  attr_accessor :credentials, :cookbooks_path, :cookbooks_tar, :remote_cache_dir

  def initialize(task)
    super(task)
    @chef_primitives_path = "#{File.expand_path(File.dirname(__FILE__))}/chef_solo_automator"
    @remote_cache_dir = "/var/cache/loom"
    @remote_chef_dir = "/var/chef"
  end

  # create local tarballs of the cookbooks, roles, data_bags, etc to be scp'd to remote machine
  def generate_chef_primitive_tar(chef_primitive)

    chef_primitive_path = "#{@chef_primitives_path}/#{chef_primitive}"
    chef_primitive_tar = "#{@chef_primitives_path}/#{chef_primitive}.tar.gz"

    # limit tarball regeneration to once per 10min
    if !File.exist?(chef_primitive_tar) or ((Time.now - File.stat(chef_primitive_tar).mtime).to_i > 600)
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
      if (k =~ /identityfile/)
        @credentials[:keys] = [ v ]
      elsif (k =~ /password/)
        @credentials[:password] = v
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

    # services is a list of services on this node
    node_services_data = @task['config']['services']
    if (node_services_data.nil? || node_services_data == "")
      node_services_data = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    end

    # merge data together into expected layout for json_attributes
    clusterdata['nodes'] = nodesdata
    servicedata['loom']['cluster'] = clusterdata
    servicedata['loom']['services'] = node_services_data

    # include the clusterId
    servicedata['loom']['clusterId'] = @task['clusterId']

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

    # do we need sudo bash?
    sudo = 'sudo' unless sshauth['user'] == 'root'

    set_credentials(sshauth)

    %w[cookbooks data_bags roles].each do |chef_primitive|
      generate_chef_primitive_tar(chef_primitive)
    end

    log.debug "Attempting ssh into ip: #{@task["config"]["ipaddress"]}, user: #{ssh-auth['user']}"

    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|

        ssh_exec!(ssh, "hostname", "Validating connectivity to #{hostname}")

        # determine if curl is installed, else default to wget
        chef_install_cmd = "curl -L https://www.opscode.com/chef/install.sh | #{sudo} bash"
        begin
          ssh_exec!(ssh, "which curl", "Checking for curl")
        rescue CommandExecutionError
          log.debug "curl not found, defaulting to wget"
          chef_install_cmd = "wget -qO - https://www.opscode.com/chef/install.sh | #{sudo} bash"
        end

        ssh_exec!(ssh, chef_install_cmd, "Installing chef")

        ssh_exec!(ssh, "type chef-solo", "Chef install validation")

        ssh_exec!(ssh, "#{sudo} mkdir -p #{@remote_cache_dir}", "Create remote cache dir")
        ssh_exec!(ssh, "#{sudo} mkdir -p #{@remote_chef_dir}", "Create remote Chef dir")

        ssh_exec!(ssh, "#{sudo} chown -R #{sshauth['user']} #{@remote_cache_dir}", "Changing cache dir owner to #{sshauth['user']}")
        ssh_exec!(ssh, "#{sudo} chown -R #{sshauth['user']} #{@remote_chef_dir}", "Changing Chef dir owner to #{sshauth['user']}")
      end
    rescue Net::SSH::AuthenticationFailed => e
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # check to ensure scp is installed and attempt to install it
    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|

        log.debug "Checking for scp installation"
        begin
          ssh_exec!(ssh, "which scp")
        rescue CommandExecutionError
          log.warn "scp not found, attempting to install openssh-client"
          scp_install_cmd = "#{sudo} yum -qy install openssh-clients"
          begin
            ssh_exec!(ssh, "which yum")
          rescue CommandExecutionError
            scp_install_cmd = "#{sudo} apt-get -qy install openssh-client"
          end
          ssh_exec!(ssh, scp_install_cmd, "installing openssh-client via #{scp_install_cmd}")
        else
          log.debug "scp found on remote"
        end
      end
    rescue Net::SSH::AuthenticationFailed => e
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # upload tarballs to target machine
    %w[cookbooks data_bags roles].each do |chef_primitive|
      log.debug "Uploading #{chef_primitive} from #{@chef_primitives_path}/#{chef_primitive}.tar.gz to #{ipaddress}:#{@remote_cache_dir}/#{chef_primitive}.tar.gz"
      begin
        Net::SCP.upload!(ipaddress, sshauth['user'], "#{@chef_primitives_path}/#{chef_primitive}.tar.gz", "#{@remote_cache_dir}/#{chef_primitive}.tar.gz", :ssh =>
            @credentials)
      rescue Net::SSH::AuthenticationFailed => e
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
      log.debug "Upload complete"
    end

    # extract tarballs on remote machine to /var/chef
    %w[cookbooks data_bags roles].each do |chef_primitive|
      begin
        Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
          ssh_exec!(ssh, "tar xf #{@remote_cache_dir}/#{chef_primitive}.tar.gz -C #{@remote_chef_dir}", "Extracting remote #{@remote_cache_dir}/#{chef_primitive}.tar.gz")
        end
      rescue Net::SSH::AuthenticationFailed => e
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
    end

    @result['status'] = 0

    log.info "ChefSoloAutomator bootstrap completed successfully: #{@result}"
    @result
  end

  def runchef(inputmap)
    sshauth = inputmap['sshauth']
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

    # do we need sudo bash?
    sudo = 'sudo' unless sshauth['user'] == 'root'

    set_credentials(sshauth)

    begin
      # write json attributes to a local tmp file
      tmpjson = Tempfile.new("loom")
      tmpjson.write(jsondata)
      tmpjson.close

      # scp task.json to remote
      log.debug "Copying json attributes to remote"
      begin
        Net::SCP.upload!(ipaddress, sshauth['user'], tmpjson.path, "#{@remote_cache_dir}/#{@task['taskId']}.json", :ssh =>
          @credentials)
      rescue Net::SSH::AuthenticationFailed
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
      log.debug "Copy json attributes complete"

    ensure
      tmpjson.close
      tmpjson.unlink
    end

    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|

        ssh_exec!(ssh, "#{sudo} chef-solo -j #{@remote_cache_dir}/#{@task['taskId']}.json -o '#{run_list}'", "Running Chef-solo")
      end
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    @result['status'] = 0
    log.info "Chef-solo run completed successfully for task #{@task['taskId']}: #{@result}"
    @result
  end

  def install(inputmap)
    log.info "ChefSoloAutomator performing install task #{@task['taskId']}"
    runchef(inputmap)
  end

  def configure(inputmap)
    log.info "ChefSoloAutomator performing configure task #{@task['taskId']}"
    runchef(inputmap)
  end

  def init(inputmap)
    log.info "ChefSoloAutomator performing initialize task #{@task['taskId']}"
    runchef(inputmap)
  end

  def start(inputmap)
    log.info "ChefSoloAutomator performing start task #{@task['taskId']}"
    runchef(inputmap)
  end

  def stop(inputmap)
    log.info "ChefSoloAutomator performing stop task #{@task['taskId']}"
    runchef(inputmap)
  end

  def remove(inputmap)
    log.info "ChefSoloAutomator performing remove task #{@task['taskId']}"
    runchef(inputmap)
  end

end

