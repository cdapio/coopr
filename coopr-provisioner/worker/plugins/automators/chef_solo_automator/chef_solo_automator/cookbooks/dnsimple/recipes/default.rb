#
# Cookbook Name:: dnsimple
# Recipe:: default
#
# Copyright 2014, Aetrion LLC
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

include_recipe 'build-essential'

value_for_platform_family(
  'debian' => ['libxml2-dev', 'libxslt1-dev'],
  'rhel' => ['libxml2-devel', 'libxslt-devel'],
).each do |pkg|
  r = package( pkg ) { action :nothing }
  r.run_action( :install )
end

chef_gem 'fog' do
  version node['dnsimple']['fog_version']
  action :install
end

require 'fog'
