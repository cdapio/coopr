#!/usr/bin/env ruby

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
  e.inspect
  e.backtrace
end

