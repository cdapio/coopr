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
require 'tmpdir'
require 'fileutils'
require 'rubygems/package'
require 'zlib'

module Loom
  class ResourceManager
    include Logging

    attr_accessor :resourcespec, :datadir, :workdir, :tenant, :active, :config

    def initialize(resourcespec, tenant, config)
      @resourcespec = resourcespec
      @config = config
      @tenant = tenant
      @datadir = %W( #{config.get(PROVISIONER_DATA_DIR)} #{tenant} ).join('/')
      @workdir = %W( #{config.get(PROVISIONER_WORK_DIR)} #{tenant} ).join('/')
      @active = {}
      # temporary
      @tenantnames = {}
      @tenantnames['e7fd030d-59f8-435b-ad5b-ce76de25d11e'] = 'tenant1'
      @tenantnames['4e987276-e87b-44eb-9b56-92413fecb40f'] = 'tenant2'
      @tenantnames['5fee6bea-9c21-4bd4-aea9-c5271451abe7'] = 'tenant3'
      @tenantnames['9ba44dcc-922f-4d58-9188-5870b60a1dda'] = 'loom'
    end

    # syncs and activates all resources, first determining which ones need to be fetched locally
    def sync
      log.debug "tenant #{@tenant} syncing resources: #{@resourcespec.resources}"
      load_active_from_disk
      log.debug "currently active: #{@active}"

      # check and sync resources
      @resourcespec.resources.each do |resource, version|
        if is_synced?(resource, version)
          log.debug "resource #{resource} version #{version} is already synced"
        else
          # sync resource locally from server
          case @resourcespec.resource_formats[resource]
          when /archive/i
            sync_archive_resource(resource, version)
          when /file/i
            sync_file_resource(resource, version)
          else
            fail "Unknown format for resource #{resource}: #{@resourcespec.resource_formats[resource]}"
          end
        end
      end

      # check and activate resources,
      @resourcespec.resources.each do |resource, version|
        if is_active?(resource, version)
          log.debug "resource #{resource} version #{version} is already active"
        else
          # activate new resource
          activate_resource(resource, version)
        end
      end
    end

    # sync a resource to the local data directory
    def sync_resource(format, resource, version)
      case format
        when 'file'
          sync_file_resource(resource, version)
        when 'archive'
          sync_archive_resource(resource, version)
      end
      log.debug "synced resource #{resource} version #{version}"
    end

    # sync a resource to the local data directory as a file
    def sync_file_resource(resource, version)
      fetch_resource(resource, version) do |download_file|
        # move downloaded file to its place in data dir
        # ex: data/tenant1/automatortype/chef-solo/roles/cluster.json/1/cluster.json
        data_file = %W( #{@datadir} #{resource} #{version} #{resource.split('/')[-1]}).join('/')
        log.debug "storing fetched file #{download_file} into data dir: #{data_file}"
        FileUtils.mkdir_p(File.dirname(data_file))
        FileUtils.mv(download_file, data_file)
      end
    end

    # sync a resource to the local data directory as an exploded archive
    def sync_archive_resource(resource, version)
      fetch_resource(resource, version) do |archive|
        dest_dir = %W( #{@datadir} #{resource} #{version} ).join('/')
        log.debug "exploding fetched archive #{archive} into data dir: #{dest_dir}"
        # process the tar.gz
        Gem::Package::TarReader.new(Zlib::GzipReader.open(archive)) do |targz|
          dest = nil
          targz.each do |entry|
            dest = File.join dest_dir, entry.full_name
            # check if any old data exists, could happen if same resource name but different format
            if File.directory? dest
              log.debug "removing existing directory (#{dest} before extracting archive there"
              FileUtils.rm_rf dest
            elsif File.file? dest.chomp('/')
              log.debug "removing existing file (#{dest.chomp()}) before extracting archive there"
              File.delete dest.chomp('/')
            end
            # extract
            if entry.directory?
              FileUtils.mkdir_p dest, :mode => entry.header.mode
            elsif entry.file?
              File.open dest, "wb" do |f|
                f.print entry.read
              end
              FileUtils.chmod entry.header.mode, dest
            elsif entry.header.typeflag == '2' # symlink
              File.symlink entry.header.linkname, dest
            end
            dest = nil
          end
        end
      end
    end

    # fetches a resource from the server to a tmp directory, yields the file location to a block
    def fetch_resource(resource, version)
      begin
        uri = %W( #{@config.get(PROVISIONER_SERVER_URI)} v1/loom #{resource} versions #{version} ).join('/')
        log.debug "fetching resource at #{uri} with tenantID #{@tenantnames[@tenant]}"
        begin
          #response = RestClient.get(uri, {'X-Loom-UserID' => 'admin', 'X-Loom-TenantID' => @tenant})
          # temporary
          response = RestClient.get(uri, {'X-Loom-UserID' => 'admin', 'X-Loom-TenantID' => @tenantnames[@tenant]})
        rescue => e
          log.error "unable to fetch resource: #{e.inspect}"
          return
        end

        log.debug "server responded with code #{response.code}"
        #tenantID should be tenantNAME currently

        tmpdir = Dir.mktmpdir
        tmpfile = %W( #{tmpdir} #{resource.split('/')[-1]} ).join('/')
        File.open(tmpfile, 'w') do |f|
          f.write response.body
        end
        yield tmpfile

      ensure
        if defined? tmpfile
          unless tmpfile.nil?
            FileUtils.rm_rf tmpfile
          end
        end
      end
    end

    # deletes a resource from the local data directory
    def delete_resource(resource, version)
      log.debug "deleting resource #{resource} #{version}"
      data_file = %W( #{@datadir} #{resource} #{version} #{resource.split('/')[-1]}).join('/')
      # delete either the file or directory from data dir
      File.delete data_file if File.file? data_file
      FileUtils.rm_rf data_file if File.directory? data_file
    end

    # activates a resource by creating a symlink from the work_dir to a particular version in the data directory
    def activate_resource(resource, version)
      unless is_synced?(resource, version)
        log.error "attempt to activate resource #{resource} version #{version} but it is not synced from server"
        return
      end
      data_file = %W( #{@datadir} #{resource} #{version} #{resource.split('/')[-1]}).join('/')
      work_link = %W( #{@workdir} #{resource} ).join('/')
      deactivate_resource(resource) if File.symlink?(work_link)
      FileUtils.mkdir_p(File.dirname(work_link))
      File.symlink data_file, work_link
      log.debug "activated resource #{resource} version #{version}"
    end

    # deactivate a resource by removing the work dir symlink
    def deactivate_resource(resource)
      work_link = %W( #{@workdir} #{resource} ).join('/')
      log.debug "deactivating: #{work_link}"
      File.delete(work_link)
      log.debug "deactivated resource #{resource}"
    end

    # get active version for a given resource
    def active_version(resource)
      work_link = %W( #{@workdir} #{resource} ).join('/')
      return nil unless File.symlink? work_link
      target = File.readlink( work_link )
      return target.split('/')[-2]
    end

    # determine if a versioned resource exists in the data dir
    def is_synced?(resource, version)
      data = %W( #{@datadir} #{resource} #{version} #{resource.split('/')[-1]}).join('/')
      File.file?(data) || File.directory?(data)
    end

    # determine if a versioned resource is active
    def is_active?(resource, version)
      return false unless is_synced?(resource, version)
      if version == active_version(resource)
        return true
      else
        return false
      end
    end

    # determine which resources and versions are currently active in the work_dir
    def load_active_from_disk
      # loop recursively through work dir
      workdir = Pathname.new(@workdir)
      if workdir.exist?
        workdir.find do |path|
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
            @active[path.relative_path_from(workdir).to_s] = version
          end
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
