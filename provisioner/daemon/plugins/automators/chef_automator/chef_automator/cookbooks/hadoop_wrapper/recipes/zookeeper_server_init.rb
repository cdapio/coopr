#
# Cookbook Name:: hadoop_wrapper
# Recipe:: zookeeper_server_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::zookeeper_server'

### TODO: zookeeper initialization requires logic to determine myid from zoocfg attribute array
myid = node['zookeeper']['myid'] ? "--myid=#{node['zookeeper']['myid']}" : ''

# do not run if version-2 directory exists.  this is the same logic as in the zookeeper-initialize script itself
execute 'initaction-zookeeper-init' do
  not_if { File.exist?("#{node['zookeeper']['zoocfg']['dataDir']}/version-2") }
  command "service zookeeper-server init #{myid}"
  timeout 300
end
