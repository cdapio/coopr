#
# Cookbook Name:: krb5
# Attributes:: default
#
# Copyright 2012, Eric G. Wolfe
# Copyright 2013, Gerald L. Hevener Jr., M.S.
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

case node['platform_family']
when 'rhel'
  default['krb5']['packages'] = %w(krb5-libs krb5-workstation pam pam_krb5 authconfig)
  default['krb5']['authconfig'] = 'authconfig --enableshadow --enablemd5 --enablekrb5 --enablelocauthorize --update'
  default['krb5']['conf_dir'] = '/etc/krb5kdc'
  default['krb5']['data_dir'] = '/var/kerberos/krb5kdc'
  default['krb5']['kadmin']['service_name'] = 'kadmin'
  default['krb5']['kadmin']['packages'] = %w(krb5-server)
  default['krb5']['kdc']['service_name'] = 'krb5kdc'
  default['krb5']['kdc']['packages'] = %w(krb5-server krb5-server-ldap)
when 'debian'
  default['krb5']['packages'] = %w(libpam-krb5 libpam-runtime libkrb5-3 krb5-user)
  default['krb5']['authconfig'] = 'pam-auth-update --package krb5'
  default['krb5']['conf_dir'] = '/etc/krb5kdc'
  default['krb5']['data_dir'] = '/var/lib/krb5kdc'
  default['krb5']['kadmin']['service_name'] = 'krb5-admin-server'
  default['krb5']['kadmin']['packages'] = %w(krb5-admin-server)
  default['krb5']['kdc']['service_name'] = 'krb5-kdc'
  default['krb5']['kdc']['packages'] = %w(krb5-kdc krb5-kdc-ldap)
when 'suse'
  default['krb5']['packages'] = %w(krb5 pam_krb5 pam-config)
  default['krb5']['authconfig'] = 'pam-config --add --krb5'
else
  default['krb5']['packages'] = []
  default['krb5']['authconfig'] = ''
  default['krb5']['kadmin']['packages'] = []
  default['krb5']['kdc']['packages'] = []
end

default['krb5']['default_logging'] = 'FILE:/var/log/krb5libs.log'
default['krb5']['default_realm'] = node['domain']
default['krb5']['realms'] = [node['domain']]
default['krb5']['default_realm_kdcs'] = []
default['krb5']['lookup_kdc'] = 'true'
default['krb5']['ticket_lifetime'] = '24h'
default['krb5']['renew_lifetime'] = '24h'
default['krb5']['forwardable'] = 'true'

default_realm =
  if node['krb5']['default_realm']
    node['krb5']['default_realm'].upcase
  elsif node['krb5']['krb5_conf']['libdefaults']['default_realm']
    ['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase
  else
    'LOCAL'
  end

# Client Packages
default['krb5']['client']['packages'] = node['krb5']['packages']
default['krb5']['client']['authconfig'] = node['krb5']['authconfig']

# logging
default['krb5']['krb5_conf']['logging']['default'] = node['krb5']['default_logging']
default['krb5']['krb5_conf']['logging']['kdc'] = 'FILE:/var/log/krb5kdc.log'
default['krb5']['krb5_conf']['logging']['admin_server'] = 'FILE:/var/log/kadmind.log'

# libdefaults
default['krb5']['krb5_conf']['libdefaults']['default_realm'] = default_realm
default['krb5']['krb5_conf']['libdefaults']['dns_lookup_realm'] = false
default['krb5']['krb5_conf']['libdefaults']['dns_lookup_kdc'] = node['krb5']['lookup_kdc']
default['krb5']['krb5_conf']['libdefaults']['forwardable'] = node['krb5']['forwardable']
default['krb5']['krb5_conf']['libdefaults']['renew_lifetime'] = node['krb5']['renew_lifetime']
default['krb5']['krb5_conf']['libdefaults']['ticket_lifetime'] = node['krb5']['ticket_lifetime']

# realms
default['krb5']['krb5_conf']['realms']['default_realm'] = node['krb5']['krb5_conf']['libdefaults']['default_realm']
default['krb5']['krb5_conf']['realms']['default_realm_kdcs'] = node['krb5']['default_realm_kdcs']
default['krb5']['krb5_conf']['realms']['default_realm_admin_server'] = ''
default['krb5']['krb5_conf']['realms']['realms'] = node['krb5']['realms']

# appdefaults
default['krb5']['krb5_conf']['appdefaults']['pam']['debug'] = false
default['krb5']['krb5_conf']['appdefaults']['pam']['forwardable'] = node['krb5']['krb5_conf']['libdefaults']['forwardable']
default['krb5']['krb5_conf']['appdefaults']['pam']['renew_lifetime'] = node['krb5']['krb5_conf']['libdefaults']['renew_lifetime']
default['krb5']['krb5_conf']['appdefaults']['pam']['ticket_lifetime'] = node['krb5']['krb5_conf']['libdefaults']['ticket_lifetime']
default['krb5']['krb5_conf']['appdefaults']['pam']['krb4_convert'] = false
