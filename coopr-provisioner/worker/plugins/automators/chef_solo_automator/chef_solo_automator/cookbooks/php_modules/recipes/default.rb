#
# Cookbook Name:: php_modules
# Recipe:: default
#
# Copyright © 2014 Cask Data, Inc.
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

# Add some build pre-requisites
include_recipe 'build-essential'

package 'zlib-devel' if node['platform_family'] == 'rhel'

# Install PHP modules, from a list
if node['php'].key?('modules')
  node['php']['modules'].each do |m|
    php_pear m do
      action :install
    end
  end
end

cookbook_file "#{node['apache']['docroot_dir']}/index.html"
  action :create_if_missing
  owner node['apache']['user']
  group node['apache']['group']
  mode '0644'
  only_if node['platform_family'] == 'rhel'
end

