# -*- coding: utf-8 -*-

include_recipe 'lxc::bugfix_precise_repo'

dpkg_autostart 'lxc' do
  allow false
end

dpkg_autostart 'lxc-net' do
  allow false
end

include_recipe 'lxc::install_dependencies'
include_recipe 'lxc::package'

# Start at 0 and increment up if found
unless(node[:network][:interfaces][:lxcbr0])
  max = node.network.interfaces.map do |int, info|
    info[:routes]
  end.flatten.compact.map do |routes|
    if(routes[:family].to_s == 'inet')
      val = (routes[:via] || routes[:destination])
      next unless val.start_with?('10.0')
      val.split('/').first.to_s.split('.')[3].to_i
    end
  end.flatten.compact.max

  node.set[:lxc][:network_device][:oct] = max ? max + 1 : 0

  # Test for existing bridge. Use different subnet if found
  l_net = "10.0.#{node[:lxc][:network_device][:oct]}"
  node.set[:lxc][:default_config][:lxc_addr] = "#{l_net}.1"
end

lxc_net_prefix = node[:lxc][:default_config][:lxc_addr].sub(%r{\.1$}, '')

Chef::Log.debug "Lxc net prefix: #{lxc_net_prefix}"

node.default[:lxc][:default_config][:lxc_network] = "#{lxc_net_prefix}.0/24"
node.default[:lxc][:default_config][:lxc_dhcp_range] = "#{lxc_net_prefix}.2,#{lxc_net_prefix}.254"
node.default[:lxc][:default_config][:lxc_dhcp_max] = '150'

file '/usr/local/bin/lxc-awesome-ephemeral' do
  action :delete
  only_if{ node[:lxc][:deprecated][:delete_awesome_ephemerals] }
end

if(node[:lxc][:proxy][:enable])
  include_recipe 'lxc::proxy'
end

file '/etc/default/lxc' do
  content lazy{
    node[:lxc][:default_config].map do |key, value|
      "#{key.to_s.upcase}=#{value.inspect}"
    end.join("\n")
  }
  mode 0644
end

if(node.platform_family?(:rhel))
  include_recipe 'lxc::rhel_bridge'
end

include_recipe 'lxc::service'

chef_gem 'elecksee' do
  if(node[:lxc][:elecksee][:version_restriction])
    version node[:lxc][:elecksee][:version_restriction]
  end
  action node[:lxc][:elecksee][:action]
end

file '/etc/apparmor.d/lxc/lxc-with-nesting' do
  path 'lxc-nesting.apparmor'
  mode 0644
  action node[:lxc][:apparmor][:enable_nested_containers] ? :create : :delete
  notifies :restart, 'service[lxc-apparmor]', :immediately
  only_if{ node.platform == 'ubuntu' }
end

require 'elecksee/lxc'
