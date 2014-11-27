# use upstart on ubuntu > saucy
service_provider = Chef::Provider::Service::Upstart if 'ubuntu' == node['platform'] &&
  Chef::VersionConstraint.new('>= 13.10').include?(node['platform_version'])

# this just reloads the dnsmasq rules when the template is adjusted
service 'lxc-net' do
  provider service_provider
  action [:enable, :start]
  subscribes :restart, 'file[/etc/default/lxc]'
  only_if{ node.platform_family?('debian') }
end

service 'lxc' do
  provider service_provider
  action [:enable, :start]
end

service 'lxc-apparmor' do
  service_name 'apparmor'
  action :nothing
end
