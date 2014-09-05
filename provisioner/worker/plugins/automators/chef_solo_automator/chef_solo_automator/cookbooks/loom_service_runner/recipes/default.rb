#
# Cookbook Name:: loom_service_runner
# Recipe:: default
#
# Copyright © 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

# We run through all of the services listed, and start/stop them, if a service resource exists

if node['loom']['node'].has_key? 'services'
  node['loom']['node']['services'].each do |k, v|
    ruby_block "service-#{v}-#{k}" do
      block do
        Chef::Log.info("Service: #{k}, action: #{v}")
        ### TODO: There's no checking if the service resource exists
        resources("service[#{k}]").run_action(v.to_sym)
      end # block
    end # ruby_block
  end # each
end # if
