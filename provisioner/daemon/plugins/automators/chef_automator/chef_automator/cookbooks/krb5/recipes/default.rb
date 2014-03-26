#
# Cookbook Name:: krb5
# Recipe:: default
#
# Copyright 2012, Eric G. Wolfe
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

node['krb5']['packages'].each do |krb5_package|
  package krb5_package
end

execute 'authconfig' do
  command node['krb5']['authconfig']
  not_if { 'grep pam_krb5 /etc/pam.d/system-auth' || 'grep pam_krb5 /etc/pam.d/common-auth' }
  action :nothing
end

template '/etc/krb5.conf' do
  owner 'root'
  group 'root'
  mode '0644'
  notifies :run, 'execute[authconfig]'
end
