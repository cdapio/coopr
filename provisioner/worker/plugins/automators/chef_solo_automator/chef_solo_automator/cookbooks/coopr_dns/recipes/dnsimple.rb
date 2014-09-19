#
# Cookbook Name:: coopr_dns
# Recipe:: dnsimple
#
# Copyright (C) 2013-2014 Continuuity, Inc.
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

%w(build-essential dnsimple).each do |recipe|
  include_recipe recipe
end

# Get credentials
if node['dnsimple']['username'] && node['dnsimple']['password']
  dnsimple = node['dnsimple']
else
  begin
    bag = node['coopr_dns']['dnsimple']['databag_name']
    item = node['coopr_dns']['dnsimple']['databag_item']
    dnsimple = data_bag_item(bag, item)
  rescue
    Chef::Application.fatal!('You must specify either a data bag or username/password!')
  end
end

# Setup some variables
fqdn = node['coopr']['hostname'] ? node['coopr']['hostname'] : node['fqdn']
hostname = fqdn.split('.').first
subdomain = fqdn.split('.', 2).last
access_v4 = node['coopr']['ipaddresses']['access_v4'] ? node['coopr']['ipaddresses']['access_v4'] : node['ipaddress']

# Do not register "private" domains
case subdomain
when 'local', 'novalocal', 'internal'
  subdomain = node['coopr_dns']['default_domain'] ? node['coopr_dns']['default_domain'] : 'local'
end

# Create DNS entries for access_v4
dnsimple_record hostname do
  content access_v4
  type 'A'
  username dnsimple['username']
  password dnsimple['password']
  domain subdomain
  ttl node['coopr_dns']['default_ttl']
  not_if { subdomain == 'local' }
end
