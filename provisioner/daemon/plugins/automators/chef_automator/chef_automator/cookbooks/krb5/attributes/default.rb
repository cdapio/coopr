#
# Cookbook Name:: krb5
# Attributes:: default
#
# Copyright 2012, Eric G. Wolfe
# Copyright 2013, Gerald L. Hevener Jr., M.S.
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
  default['krb5']['packages'] = %w[krb5-libs krb5-workstation pam pam_krb5 authconfig]
  default['krb5']['authconfig'] = 'authconfig --enableshadow --enablemd5 --enablekrb5 --enablelocauthorize --update'
when 'debian'
  default['krb5']['packages'] = %w[libpam-krb5 libpam-runtime libkrb5-3 krb5-user]
  default['krb5']['authconfig'] = 'pam-auth-update --package krb5'
when 'suse'
  default['krb5']['packages'] = %w[krb5 pam_krb5 pam-config]
  default['krb5']['authconfig'] = 'pam-config --add --krb5'
else
  default['krb5']['packages'] = []
  default['krb5']['authconfig'] = ''
end

default['krb5']['default_logging'] = 'FILE:/var/log/krb5libs.log'
default['krb5']['default_realm'] = node['domain']
default['krb5']['realms'] = [node['domain']]
default['krb5']['default_realm_kdcs'] = []
default['krb5']['lookup_kdc'] = 'true'
default['krb5']['ticket_lifetime'] = '24h'
default['krb5']['renew_lifetime'] = '24h'
default['krb5']['forwardable'] = 'true'
