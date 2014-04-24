#
# Cookbook Name:: hadoop_wrapper
# Recipe:: zookeeper_server_init
#
# Copyright (C) 2013 Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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
