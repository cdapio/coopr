#
# Cookbook Name:: krb5
# Attributes:: kdc
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

default_realm = node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase

# kdcdefaults
default['krb5']['kdc_conf']['kdcdefaults']['kdc_ports'] = '88'
default['krb5']['kdc_conf']['kdcdefaults']['kdc_tcp_ports'] = '88'

# realms
default['krb5']['kdc_conf']['realms'][default_realm]['acl_file'] = "#{node['krb5']['conf_dir']}/kadm5.acl"
default['krb5']['kdc_conf']['realms'][default_realm]['admin_keytab'] = "FILE:#{node['krb5']['conf_dir']}/kadm5.keytab"
default['krb5']['kdc_conf']['realms'][default_realm]['database_name'] = "#{node['krb5']['data_dir']}/principal"
default['krb5']['kdc_conf']['realms'][default_realm]['key_stash_file'] = "#{node['krb5']['data_dir']}/.k5.#{default_realm}"
