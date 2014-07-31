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

module Loom
  # simple specification for a tenant's required resources
  class ResourceSpec
    attr_accessor :resources, :resource_formats

    # initialized from the json object per API spec
    # {"automatortype"=>{"chef-solo"=>{"cookbooks"=>{"format"=>"archive",
    #   "active"=>[{"name"=>"reactor", "version"=>"2"}, {"name"=>"hadoop", "version"=>"52"}]}}}}
    def initialize(resource_jsonobj)
      # resources is a hash of '/'-delimited resource paths and their versions
      #   ex: "automatortype/chef-solo/cookbooks/hadoop" => 2
      @resources = {}
      # resource_formats is a hash of '/'-delimited resource types and their formats
      #   ex: "automatortype/chef-solo/cookbooks" => "archive"
      @resource_formats = {}
      resource_jsonobj.each do |type, h|
        h.each do |id, h|
          h.each do |resource_type, h|
            if h.key?('format')
              formatkey = %W( #{type} #{id} #{resource_type} ).join('/')
              @resource_formats[formatkey] = h['format'] # Albert: Validate here?
            end
            if h.key?('active')
              h['active'].each do |nv|
                name = nv['name']
                version = nv['version']
                resource_name = %W( #{type} #{id} #{resource_type} #{name}).join('/')
                @resources[resource_name] = version
              end
            end
          end
        end
      end
    end
  end
end