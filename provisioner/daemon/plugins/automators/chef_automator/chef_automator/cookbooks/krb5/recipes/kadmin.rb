#
# Cookbook Name:: krb5
# Recipe:: kadmin
#
# Copyright 2014, Continuuity, Inc.
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
  package krb5_package
end

case node['platform_family']
when 'rhel'
  kdc_dir = '/var/kerberos/krb5kdc'
  kadm_svc = 'kadmin'
when 'debian'
  kdc_dir = '/var/lib/krb5kdc'
  kadm_svc = 'krb5-admin-server'
end

default_realm = node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase

template node['krb5']['kdc_conf']['realms'][default_realm]['acl_file'] do
  owner 'root'
  group 'root'
  mode '0644'
  not_if { node['krb5']['kadm5_acl'].empty? }
end

log 'create-krb5-db' do
  message 'Creating Kerberos Database... this may take a while...'
  level :info
  not_if "test -e #{kdc_dir}/principal"
end

execute 'create-krb5-db' do
  command "echo '#{node['krb5']['master_password']}\n#{node['krb5']['master_password']}\n' | kdb5_util create -s"
  not_if "test -e #{kdc_dir}/principal"
end

execute 'create-admin-principal' do
  command "echo '#{node['krb5']['admin_password']}\n#{node['krb5']['admin_password']}\n' | kadmin.local -q 'addprinc #{node['krb5']['admin_principal']}'"
  not_if "kadmin.local -q 'list_principals' | grep -e ^#{node['krb5']['admin_principal']}"
end

include_recipe 'krb5::kdc'

service 'krb5-admin-server' do
  service_name kadm_svc
  action :start
end
