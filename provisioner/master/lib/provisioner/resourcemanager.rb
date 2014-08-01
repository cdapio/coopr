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

require_relative 'logging'

require 'pathname'
require 'rest_client'

module Loom
  class ResourceManager
    include Logging

    attr_accessor :resourcespec, :datadir, :workdir, :tenant, :active, :config

    def initialize(resourcespec, config)
      @resourcespec = resourcespec
      @config = config
      #puts `pwd`
      @datadir = 'tmp'
      @workdir = Pathname.new('../../../var/work')
      @tenant = 'tenant1'
      @active = {}
    end

    def sync
      log.debug "tenant #{@tenant} syncing resources: #{@resourcespec.resources}"
      # compare spec vs whats on disk
      # make the api call to sync each resource
      sleeptime = Random.rand(30)
      puts "simulating sync for #{sleeptime} sec"
      sleep sleeptime
      puts "downloading for real"
      uri = "http://localhost:55054/v1/loom/automatortypes/chef-solo/cookbooks/zookeeper/versions/1"
      response = RestClient.get(uri, {'X-Loom-UserID' => 'admin', 'X-Loom-TenantID' => 'loom'})
      puts "downloaded file"
      File.open("/tmp/log/download.#{sleeptime}.tgz", 'w') do |f|
        f.write response.body
      end


    end

    def load_active_from_disk
      # loop recursively through work dir
      @workdir.find do |path|
        #puts "path: #{path}"
        #puts path.relative_path_from(@workdir)
        # symlinks indicate an active version of a resource
        if File.symlink?( path )
          #puts "^^^ link"
          resource_name = File.basename(path)
          #puts "name: #{resource_name}"
          # determine where the link points
          target = File.readlink( path )
          #puts "  target: #{target}"
          # the version will be the parent directory
          version = target.split('/')[-2]
          #puts "    version: #{version}"
          @active[path.relative_path_from(@workdir)] = version
        end
      end
    end


  end
end

if __FILE__ == $PROGRAM_NAME
  puts `pwd`
  rm = Loom::ResourceManager.new
  rm.load_active_from_disk

  rm.active.each do |k, v|
    puts "#{k.to_s}: #{v}"
  end
end
