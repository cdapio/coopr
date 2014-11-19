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

require 'json'
require_relative '../lib/provisioner/rest-helper'

id = ARGV.shift
num_workers = ARGV.shift

if (id.nil? || num_workers.nil?)
  puts "must supply tenant_id and num_workers as arguments"
  exit 1
end

data = Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
data['id'] = id
data['workers'] = num_workers.to_i
#data['resources']['automatortypes']['chef-solo']['cookbooks']['format'] = 'archive'
#data['resources']['automatortypes']['chef-solo']['cookbooks']['active'] = [
#      {"name" => "hadoop", "version" => '2'}
#    ]
#data['resources']['automatortypes']['chef-solo']['roles']['format'] = 'file'
#data['resources']['automatortypes']['chef-solo']['roles']['active'] = [
#      {"name" => "testrole.json", "version" => '2'},
#      {"name" => "anotherrole.json", "version" => '1'}
#    ]
#data['resources']['automatortypes']['chef-solo']['data_bags']['format'] = 'archive'
#data['resources']['automatortypes']['chef-solo']['data_bags']['active'] = [
#      {"name" => "users", "version" => '2'}
#    ]

begin
  json = JSON.generate(data)
  resp = Coopr::RestHelper.put("http://localhost:55056/v2/tenants/#{id}", json)
  if (resp.code == 200)
    puts 'success: 200'
  else
    puts "response code: #{resp.code}"
  end
rescue => e
  puts 'Caught exception'
  puts e.inspect
  puts e.backtrace
end
