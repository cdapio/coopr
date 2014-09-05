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

module Coopr 
  # simple specification for a tenant's required resources
  class ResourceSpec
    attr_accessor :resources, :resource_formats, :resource_permissions

    # initialized from the json object per API spec
    # {"automatortype"=>{"chef-solo"=>{"cookbooks"=>{"format"=>"archive",
    #   "active"=>[{"name"=>"reactor", "version"=>"2"}, {"name"=>"hadoop", "version"=>"52"}]}}}}
    def initialize(resource_jsonobj)
      # resources is a hash of '/'-delimited resource paths and their versions
      #   ex: "automatortype/chef-solo/cookbooks/hadoop" => 2
      @resources = {}
      # resource_formats is a hash of '/'-delimited resource paths and their formats
      #   ex: "automatortype/chef-solo/cookbooks/hadoop" => "archive"
      @resource_formats = {}
      # resource_permissions is a hash of '/'-delimited resource paths and their file permissions
      # only applicable to resources with format = 'file'
      #   ex: "automatortype/shell/scripts/my_script.sh" => "0755"
      @resource_permissions = {}
      unless resource_jsonobj.nil?
        # example resource_jsonobj: "automatortype" => {...}
        resource_jsonobj.each do |type, h_type|
          next if h_type.nil?
          # example h_type hash: "chef-solo" => {...}
          h_type.each do |id, h_id|
            next if h_id.nil?
            # example h_id hash: "cookbooks" => {"format" => "...", "active => [...]"}
            h_id.each do |resource_type, h_resource|
              next if h_resource.nil?
              format = nil
              if h_resource.key?('format')
                format = h_resource['format']
              end
              permissions = nil
              if h_resource.key?('permissions')
                permissions = h_resource['permissions']
              end
              if h_resource.key?('active')
                # example h_resource['active'] array: [{"name" => "hadoop", "version" => "2"}]
                h_resource['active'].each do |nv|
                  name = nv['name']
                  version = nv['version']
                  resource_name = %W( #{type} #{id} #{resource_type} #{name}).join('/')
                  @resources[resource_name] = version
                  @resource_formats[resource_name] = format
                  @resource_permissions[resource_name] = permissions unless permissions.nil?
                end
              end
            end
          end
        end
      end
    end

    # define two ResourceSpecs as equal if @resources hash has same contents
    def ==(other)
      @resources == other.resources && @resource_formats == other.resource_formats \
                    && @resource_permissions == other.resource_permissions
    end
  end
end
