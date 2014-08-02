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
      # resource_formats is a hash of '/'-delimited resource paths and their formats
      #   ex: "automatortype/chef-solo/cookbooks/hadoop" => "archive"
      @resource_formats = {}
      unless resource_jsonobj.nil?
        resource_jsonobj.each do |type, h|
          next if h.nil?
          h.each do |id, h|
            next if h.nil?
            h.each do |resource_type, h|
              next if h.nil?
              format = nil
              if h.key?('format')
                format = h['format']
              end
              if h.key?('active')
                h['active'].each do |nv|
                  name = nv['name']
                  version = nv['version']
                  resource_name = %W( #{type} #{id} #{resource_type} #{name}).join('/')
                  @resources[resource_name] = version
                  @resource_formats[resource_name] = format
                end
              end
            end
          end
        end
      end
    end

    # define two ResourceSpecs as equal if @resources hash has same contents
    def ==(other)
      @resources == other.resources && @resource_formats == other.resource_formats
    end
  end
end