#
# Cookbook Name:: sysctl
# Recipe:: ohai_plugin
#
# Copyright 2014, OneHealth Solutions, Inc.
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

ohai 'reload_sys' do
  plugin 'sys'
  action :nothing
end

cookbook_file "#{node['ohai']['plugin_path']}/sys.rb" do
  source 'plugins/sys.rb'
  owner 'root'
  group node['root_group']
  mode '0755'
  notifies :reload, 'ohai[reload_sys]', :immediately
end

include_recipe 'ohai::default'
