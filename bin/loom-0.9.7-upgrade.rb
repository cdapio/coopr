#!/usr/bin/env ruby    

require 'json'
require 'optparse'

=begin
The following conversions are made to the JSON:

--- provider:
  old:
    {
        "description": "Rackspace Iaas Provider",
        "name": "rackspace",
        "providertype": "rackspace",
        "provisioner": {
            "auth": {
                "rackspace_api_key": "58cf02d1807a0c90a1fc68cc8eeef160",
                "rackspace_username": "continuuity"
            }
        }
    },

  new:
    {
      "name": "rackspace",
      "description": "Rackspace Public Cloud",
      "providertype": "rackspace",
      "provisioner": {
        "rackspace_username": "USERNAME",
        "rackspace_api_key": "API_KEY",
        "rackspace_region": "dfw"
      }
    }

--- service dependency
  old:
        {
            "dependson": [
                "base"
            ],
            "description": "Apache HTTP Server",
            "name": "apache-httpd",

  new:
        {
            "dependencies": {
                "runtime": {
                    "requires": [
                        "base"
                    ]
                }
            },
            "description": "Apache HTTP Server",
            "name": "apache-httpd",

--- service action for providertype chef
  old:
            "provisioner": {
                "actions": {
                    "start": {
                        "data": "{\"loom\": { \"node\": { \"services\": { \"apache2\": \"start\" } } } }",
                        "script": "recipe[apache2::default],recipe[loom_service_runner::default]",
                        "type": "chef"
                    },

  new:
      "provisioner": {
        "actions": {
          "start": {
            "type": "chef",
            "fields": {
              "run_list": "recipe[apache2::default],recipe[loom_service_runner::default]",
              "json_attributes": "{\"loom\": { \"node\": { \"services\": { \"apache2\": \"start\" } } } }"
            }
          },

--- service action for providertype shell
  old:
      "provisioner": {
        "actions": {
          "install": {
            "type": "shell",
            "fields": {
              "script": "/var/cache/loom/scripts/chef_client_bootstrap.sh",
              "args": "https://mychefserver:443 \"role[base]\\\""
            }
          }
        }

  new:
      "provisioner": {
        "actions": {
          "install": {
            "type": "shell",
            "fields": {
              "script": "/var/cache/loom/scripts/chef_client_bootstrap.sh",
              "args": "https://mychefserver:443 \"role[base]\\\""
            }
          }
        }

=end

# Parse command line options. 
options = {}
OptionParser.new do |opts|
  opts.banner = 'Utility to convert a Loom pre-0.9.7 export JSON file to a 0.9.7-compatible import JSON file.  Usage: '
  opts.on('-f', '--file FILE', 'Full path to task json.  Required.') do |f|
    options[:file] = f
  end
  opts.on('-o', '--output FILE', 'Full path to output file.  Defaults to STDOUT.') do |o|
    options[:output] = o
  end
end.parse!

if(!options[:file])
  puts "You must specify an input JSON file using -f [file]"
  exit(1)
end

begin
  task = JSON.parse( IO.read(options[:file]) )
  output = task

  task.each do |k, v|
    case k
    when "services"
      v.each do |s|
        # update service dependencies
        if s.key?('dependson')
          s['dependencies'] = Hash.new unless s.key?('dependencies')
          s['dependencies']['runtime'] = Hash.new unless s['dependencies'].key?('runtime')
          s['dependencies']['runtime']['requires'] = s['dependson']
          s.delete('dependson')
        end
        # update service actions for chef and shell plugins 
        if s.key?('provisioner') && s['provisioner'].key?('actions')
          s['provisioner']['actions'].each do |k, v|
            case v['type']
            when "chef", "chef-solo"
              if v.key?('script') 
                v['fields'] = Hash.new unless v.key?('fields')
                v['fields']['run_list'] = v['script']
                v.delete('script')
              end
              if v.key?('data')
                v['fields'] = Hash.new unless v.key?('fields')
                v['fields']['json_attributes'] = v['data']
                v.delete('data')
              end
            when "shell"
              if v.key?('script') 
                v['fields'] = Hash.new unless v.key?('fields')
                v['fields']['script'] = v['script']
                v.delete('script')
              end
              if v.key?('data')
                v['fields'] = Hash.new unless v.key?('fields')
                v['fields']['args'] = v['data']
                v.delete('data')
              end
            end
          end
        end
      end
    when "providers"
      # update provider fields
      v.each do |k|
        k.each do |k, v|
          case k
          when "provisioner"
            if v.key?('auth')
              v['auth'].each do |pkey, pval|
                v[pkey] = pval
              end
              v.delete('auth')
            end
          end
        end
      end
    end
  end

  if(options[:output])
    File.open(options[:output],"w") do |f|
      f.write(JSON.pretty_generate(task))
    end
    puts "Output written to #{options[:output]}.  This file can be imported back into Loom via the UI."
  else
    puts JSON.pretty_generate(task)
  end

rescue Errno::ENOENT
  puts "Cannot open input file #{options[:file]}"
  puts $!.inspect, $@

rescue 
  puts "Unknown error"
  puts $!.inspect, $@
end

