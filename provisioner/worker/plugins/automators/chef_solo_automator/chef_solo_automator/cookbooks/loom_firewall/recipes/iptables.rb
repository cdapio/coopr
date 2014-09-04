#
# Cookbook Name:: loom_firewall
# Recipe:: iptables
#
# Copyright © 2013, Continuuity, Inc.
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

package 'iptables'

case node['platform_family']
when 'debian'
  iptable_rules = '/etc/iptables-rules'
  file "/etc/network/if-up.d/iptables-rules" do
    owner 'root'
    group 'root'
    mode '0755'
    content "#!/bin/bash\niptables-restore < #{iptable_rules}\n"
    action :create
  end
when 'rhel'
  iptable_rules = '/etc/sysconfig/iptables'
  service 'iptables' do
    supports [:restart, :reload, :status]
    action :enable
  end
end

execute 'reload-iptables' do
  command "iptables-restore < #{iptable_rules}"
  user 'root'
  action :nothing
end

template iptable_rules do
  source 'iptables.erb'
  notifies :run, 'execute[reload-iptables]'
  action :create
end
