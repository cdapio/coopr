#
# Cookbook Name:: krb5
# Recipe:: kadmin_init
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
include_recipe 'krb5::kadmin'

default_realm = node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase

log 'create-krb5-db' do
  message 'Creating Kerberos Database... this may take a while...'
  level :info
  not_if "test -e #{node['krb5']['kdc_conf']['realms'][default_realm]['database_name']}"
end

execute 'create-krb5-db' do
  command "echo '#{node['krb5']['master_password']}\n#{node['krb5']['master_password']}\n' | kdb5_util -r #{default_realm} create -s"
  not_if "test -e #{node['krb5']['kdc_conf']['realms'][default_realm]['database_name']}"
end

execute 'create-admin-principal' do
  command "echo '#{node['krb5']['admin_password']}\n#{node['krb5']['admin_password']}\n' | kadmin.local -q 'addprinc #{node['krb5']['admin_principal']}'"
  not_if "kadmin.local -q 'list_principals' | grep -e ^#{node['krb5']['admin_principal']}"
end
