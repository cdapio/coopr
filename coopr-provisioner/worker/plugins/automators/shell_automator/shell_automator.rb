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

require 'net/scp'
require 'base64'
require 'fileutils'

class ShellAutomator < Automator
  attr_accessor :credentials, :scripts_dir, :scripts_tar, :remote_cache_dir

  # plugin defined resources
  @ssh_key_dir = 'ssh_keys'
  class << self
    attr_accessor :ssh_key_dir
  end

  def initialize(env, task)
    super(env, task)
    work_dir = @env[:work_dir]
    tenant = @env[:tenant]

    @resources = %w( scripts archives )
    @resources_path = %W[ #{work_dir} #{tenant} automatortypes shell ].join('/')

    # local and remote top-level lib directory name
    @lib_parent_dir = "lib"
    # local lib dir
    @lib_dir = File.join( File.dirname(__FILE__), @lib_parent_dir)
    # name of tarball to generate
    @lib_tar = %W[ #{work_dir} #{tenant} automatortypes shell lib.tar.gz ].join('/')

    # remote storage directory
    @remote_cache_dir = "/var/cache/coopr/shell_automator"
    # remote script location to be exported in $PATH
    @remote_scripts_dir = "#{@remote_cache_dir}/scripts"
    # remote lib location
    @remote_lib_dir = "#{@remote_cache_dir}/#{@lib_parent_dir}"
    # coopr wrapper for common functions
    @wrapper_script = "#{@remote_lib_dir}/coopr_wrapper.sh"
  end

  # tar up a directory
  #   file: full path of destination tar.gz
  #   path: full path to directory, parent dir will be used as cwd
  def generate_tar(file, path)
    return if File.exist?(file) && ((Time.now - File.stat(file).mtime).to_i < 600)
    log.debug "Generating #{file} from #{path}"
    `tar -chzf "#{file}.new" -C "#{File.dirname(path)}" #{File.basename(path)}`
    `mv "#{file}.new" "#{file}"`
    log.debug "Generation complete: #{file}"
  end

  def write_ssh_file
    @ssh_keyfile = @task['config']['provider']['provisioner']['ssh_keyfile']
    unless @ssh_keyfile.nil?
      @task['config']['ssh-auth']['identityfile'] = File.join(Dir.pwd, self.class.ssh_key_dir, @task['taskId'])
      log.debug "Writing out @ssh_keyfile to #{@task['config']['ssh-auth']['identityfile']}"
      decode_string_to_file(@ssh_keyfile, @task['config']['ssh-auth']['identityfile'])
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

  def decode_string_to_file(string, outfile, mode = 0600)
    FileUtils.mkdir_p(File.dirname(outfile))
    File.open(outfile, 'wb', mode) { |f| f.write(Base64.decode64(string)) }
  end

  def runshell(inputmap)
    sshauth = inputmap['sshauth']
    ipaddress = inputmap['ipaddress']
    fields = inputmap['fields']

    raise "required parameter \"script\" not found in input: #{fields}" if fields['script'].nil?
    shellscript = fields['script']
    shellargs = fields['args']

    # do we need sudo bash?
    sudo = 'sudo' unless sshauth['user'] == 'root'

    write_ssh_file
    @ssh_file = @task['config']['ssh-auth']['identityfile'] unless @ssh_keyfile.nil?
    set_credentials(sshauth)

    jsondata = JSON.generate(task)

    # copy the task json data to the cache dir on the remote machine
    begin
      # write json task data to a local tmp file
      tmpjson = Tempfile.new("coopr")
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

    # execute the defined shell script
    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
        ssh_exec!(ssh, 
                  "cd #{@remote_scripts_dir}; export PATH=$PATH:#{@remote_scripts_dir}; #{sudo} #{@wrapper_script} #{@remote_cache_dir}/#{@task['taskId']}.json #{shellscript} #{shellargs}",
                  "Running shell command #{shellscript} #{shellargs}")
      end
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    @result['status'] = 0
    log.debug "Result of shell command: #{@result}"
    @result
  ensure
    File.delete(@ssh_file) if @ssh_file && File.exist?(@ssh_file)
  end

  def bootstrap(inputmap)
    sshauth = inputmap['sshauth']
    ipaddress = inputmap['ipaddress']

    # do we need sudo bash?
    sudo = 'sudo' unless sshauth['user'] == 'root'

    write_ssh_file
    @ssh_file = @task['config']['ssh-auth']['identityfile'] unless @ssh_keyfile.nil?
    set_credentials(sshauth)

    # generate the local tarballs for resources and for our own wrapper libs
    @resources.each do |resource|
      tar = %W[ #{@resources_path} #{resource}.tar.gz ].join('/')
      path = %W[ #{@resources_path} #{resource} ].join('/')
      generate_tar(tar, path)
    end
    generate_tar(@lib_tar, @lib_dir)

    # check to ensure scp is installed and attempt to install it
    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
        log.debug "Checking for scp installation"
        begin
          ssh_exec!(ssh, "which scp", "Checking for scp")
        rescue CommandExecutionError
          log.warn "scp not found, attempting to install openssh-client"
          scp_install_cmd = "#{sudo} yum -qy install openssh-clients"
          begin
            ssh_exec!(ssh, "which yum", "Checking for yum")
          rescue CommandExecutionError
            scp_install_cmd = "#{sudo} apt-get -qy install openssh-client"
          end
          ssh_exec!(ssh, scp_install_cmd, "installing openssh-client via #{scp_install_cmd}")
        else
          log.debug "scp found on remote"
        end
      end
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
        ssh_exec!(ssh, "#{sudo} mkdir -p #{@remote_cache_dir}", "Creating remote cache dir")
        ssh_exec!(ssh, "#{sudo} chown -R #{sshauth['user']} #{@remote_cache_dir}", "Changing cache dir owner to #{sshauth['user']}")
      end
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # scp resource tarballs to target machine
    @resources.each do |resource|
      log.debug "Uploading #{resource} from #{@resources_path}/#{resource}.tar.gz to #{ipaddress}:#{@remote_cache_dir}/#{resource}.tar.gz"
      begin
        Net::SCP.upload!(ipaddress, sshauth['user'], "#{@resources_path}/#{resource}.tar.gz", "#{@remote_cache_dir}/#{resource}.tar.gz", :ssh =>
          @credentials, :verbose => true)
      rescue Net::SSH::AuthenticationFailed
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
      log.debug "Upload complete"
    end

    # extract resource tarballs to target machine
    @resources.each do |resource|
      begin
        Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
          ssh_exec!(ssh, "tar xf #{@remote_cache_dir}/#{resource}.tar.gz -C #{@remote_cache_dir}", "Extract remote #{@remote_cache_dir}/#{resource}.tar.gz")
        end
      rescue Net::SSH::AuthenticationFailed
        raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
      end
    end

    # scp lib tarball to target machine
    begin
      Net::SCP.upload!(ipaddress, sshauth['user'], "#{@lib_tar}", "#{@remote_cache_dir}/lib.tar.gz", :ssh =>
          @credentials, :verbose => true)
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    # extract lib tarball on remote machine
    begin
      Net::SSH.start(ipaddress, sshauth['user'], @credentials) do |ssh|
        ssh_exec!(ssh, "tar xf #{@remote_cache_dir}/lib.tar.gz -C #{@remote_cache_dir}", "Extract remote #{@remote_cache_dir}/lib.tar.gz")
      end
    rescue Net::SSH::AuthenticationFailed
      raise $!, "SSH Authentication failure for #{ipaddress}: #{$!}", $!.backtrace
    end

    @result['status'] = 0
    log.debug "ShellAutomator bootstrap completed successfully: #{@result}"
  ensure
    File.delete(@ssh_file) if @ssh_file && File.exist?(@ssh_file)
  end

  def install(inputmap)
    runshell(inputmap)
  end

  def configure(inputmap)
    runshell(inputmap)
  end

  def init(inputmap)
    runshell(inputmap)
  end

  def start(inputmap)
    runshell(inputmap)
  end

  def stop(inputmap)
    runshell(inputmap)
  end

  def remove(inputmap)
    runshell(inputmap)
  end

end

