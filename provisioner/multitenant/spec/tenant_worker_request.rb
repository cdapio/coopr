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
require 'rest_client'

id = ARGV.shift
num_workers = ARGV.shift

if (id.nil? || num_workers.nil?)
  puts "must supply tenant_id and num_workers as arguments"
  exit 1
end

data = {}
data['id'] = id
data['workers'] = num_workers.to_i

begin
  json = JSON.generate(data)
  resp = RestClient.put("http://localhost:4567/v1/tenants/#{id}", json)
  if(resp.code == 200)
    puts "success: 200"
  else
    puts "response code: #{resp.code}"
  end
rescue => e
  puts "Caught exception"
  puts e.inspect
  puts e.backtrace
end

