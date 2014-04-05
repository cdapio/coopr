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

# KDC Packages
case node['platform_family']
when 'rhel'
  default['krb5']['kdc']['packages'] = %w(krb5-server)
  kdc_dir = '/var/kerberos/krb5kdc'
  etc_dir = kdc_dir
when 'debian'
  default['krb5']['kdc']['packages'] = %w(krb5-kdc krb5-kdc-ldap)
  etc_dir = '/etc/krb5kdc'
else
  default['krb5']['kdc']['packages'] = []
end

default_realm = node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase

# kdcdefaults
default['krb5']['kdc_conf']['kdcdefaults']['kdc_ports'] = '88'
default['krb5']['kdc_conf']['kdcdefaults']['kdc_tcp_ports'] = '88'

# realms
default['krb5']['kdc_conf']['realms'][default_realm]['acl_file'] = "#{etc_dir}/kadm5.acl"
default['krb5']['kdc_conf']['realms'][default_realm]['admin_keytab'] = "FILE:#{etc_dir}/kadm5.keytab"
