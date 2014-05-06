#
# Cookbook Name:: krb5_utils
# Recipe:: default
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

include_recipe 'krb5::default'

directory node['krb5_utils']['keytabs_dir'] do
  owner "root"
  group "root"
  mode "0755"
  action :create
end

execute "kdestroy" do
  command "kdestroy"
  action :run
end

execute "kinit-as-admin-user" do
  command "echo #{node['krb5_utils']['admin_password']} | kinit #{node['krb5_utils']['admin_principal']}"
  action :run
end

keytab_dir = node['krb5_utils']['keytabs_dir']

# Generate execute blocks
%w[ krb5_service_keytabs krb5_user_keytabs ].each do |kt|
  node['krb5_utils'][kt].each do |name, opts|
    case kt
    when 'krb5_service_keytabs'
      http_principal = "HTTP/#{node['fqdn']}@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
      principal = "#{name}/#{node['fqdn']}@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
      keytab_file = "#{name}.service.keytab"
    when 'krb5_user_keytabs'
      http_principal = ''
      principal = "#{name}@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
      keytab_file = "#{name}.keytab"
    end

    execute "krb5-addprinc-#{principal}" do
      command "kadmin -w #{node['krb5_utils']['admin_password']} -q 'addprinc -randkey #{principal}'"
      action :run
      not_if "kadmin -w #{node['krb5_utils']['admin_password']} -q 'list_principals' | grep -v Auth | grep '^#{principal}'"
    end

    execute "krb5-check-#{principal}" do
      command "kadmin -w #{node['krb5_utils']['admin_password']} -q 'list_principals' | grep -v Auth | grep '^#{principal}'"
      action :run
      not_if "test -e #{keytab_dir}/#{keytab_file}"
    end

    execute "krb5-generate-keytab-#{keytab_file}" do
      command "kadmin -w #{node['krb5_utils']['admin_password']} -q 'xst -kt #{keytab_dir}/#{keytab_file} #{principal} #{http_principal}'"
      action :run
      not_if "test -e #{keytab_dir}/#{keytab_file}"
    end

    file "#{keytab_dir}/#{keytab_file}" do
      owner opts.owner
      group opts.group
      mode opts.mode
      action :create
      only_if "test -e #{keytab_dir}/#{keytab_file}"
    end
  end
end
