#
# Cookbook Name:: krb5
# Recipe:: kadmin
#
# Copyright © 2014 Cask Data, Inc.
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

include_recipe 'krb5::default'

node.default['krb5']['krb5_conf']['realms']['default_realm_admin_server'] = node['fqdn']

node['krb5']['kadmin']['packages'].each do |krb5_package|
  next if node['krb5']['kdc']['packages'].include? krb5_package
  package krb5_package
end

default_realm = node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase

template node['krb5']['kdc_conf']['realms'][default_realm]['acl_file'] do
  owner 'root'
  group 'root'
  mode '0644'
  not_if { node['krb5']['kadm5_acl'].empty? }
end

include_recipe 'krb5::kdc'
