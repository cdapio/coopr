#
# Cookbook Name:: modules
# Author:: Guilhem Lettron <guilhem.lettron@youscribe.com>
#
# Copyright 20012, Societe Publica.
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

if not supported?
  return
end

node['modules']['packages'].each do |p|
  package p
end

directory "/etc/modules-load.d" do
  owner "root"
  group "root"
  mode "0644"
  action :create
end

cookbook_file "/etc/modules-load.d/header" do
  source "modules-load_header"
  owner "root"
  group "root"
  mode "0644"
end

# using upstart
case node['platform']
when "ubuntu"
  cookbook_file "/etc/init/modules-load.conf" do
    source "modules-load.conf"
    owner "root"
    group "root"
    mode "0644"
  end

  service "module-init-tools" do
    provider Chef::Provider::Service::Upstart
  end
  
  service "modules-load" do
    provider Chef::Provider::Service::Upstart
    action [:enable, :start]
    notifies :start, "service[module-init-tools]"
  end
else
  return
end

template "/etc/modules-load.d/chef-default.conf" do
  source "modules.conf.erb"
  mode "0644"
  owner "root"
  group "root"
  variables(
    :modules => node['modules']['default']['modules']
  )
  notifies :start, "service[modules-load]"
  only_if { node['modules']['default']['modules'] }
end
