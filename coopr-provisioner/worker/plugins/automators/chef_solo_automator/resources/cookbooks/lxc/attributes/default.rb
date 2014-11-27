default[:lxc][:start_ipaddress] = nil
default[:lxc][:validator_pem] = nil
default[:lxc][:auto_start] = true
default[:lxc][:bridge] = 'lxcbr0'
default[:lxc][:use_bridge] = true
default[:lxc][:addr] = '10.0.4.1'
default[:lxc][:netmask] = '255.255.255.0'
default[:lxc][:network] = '10.0.4.0/24'
default[:lxc][:dhcp_range] = '10.0.4.2,10.0.4.254'
default[:lxc][:dhcp_max] = '253'
default[:lxc][:shutdown_timeout] = 120
default[:lxc][:allowed_types] = %w(debian ubuntu fedora centos)
default[:lxc][:container_directory] = '/var/lib/lxc'
default[:lxc][:dnsmasq_lease_file] = '/var/lib/misc/dnsmasq.leases'

default[:lxc][:proxy][:enable] = false

default[:lxc][:elecksee][:version_restriction] = '~> 1.0.20'
default[:lxc][:elecksee][:action] = :install

default[:lxc][:default_config][:lxc_auto] = node[:lxc][:auto_start]
default[:lxc][:default_config][:use_lxc_bridge] = node[:lxc][:use_bridge]
default[:lxc][:default_config][:lxc_bridge] = node[:lxc][:bridge]
default[:lxc][:default_config][:lxc_addr] = node[:lxc][:addr]
default[:lxc][:default_config][:lxc_netmask] = node[:lxc][:netmask]
default[:lxc][:default_config][:lxc_dhcp_range] = node[:lxc][:dhcp_range]
default[:lxc][:default_config][:lxc_dhcp_max] = node[:lxc][:dhcp_max]
default[:lxc][:default_config][:lxc_shutdown_timeout] = node[:lxc][:shutdown_timeout]
default[:lxc][:default_config][:mirror] = node[:lxc][:mirror] || 'http://archive.ubuntu.com/ubuntu'

default[:lxc][:knife] = {}
default[:lxc][:knife][:static_range] = ''
default[:lxc][:knife][:static_ips] = []

default[:lxc][:user_locks] = %w(ubuntu)

default[:lxc][:packages] = node.platform_family?('rhel') ? ['lxc', 'lxc-templates', 'lxc-libs', 'bridge-utils', 'libcgroup'] : ['lxc']
default[:lxc][:mirror] = 'http://archive.ubuntu.com/ubuntu'
default[:lxc][:containers] = {}

default[:lxc][:deprecated][:delete_awesome_ephemerals] = true
default[:lxc][:apparmor][:enable_nested_containers] = false

# https://bugs.launchpad.net/ubuntu/+bug/1007439
# https://bugs.launchpad.net/ubuntu/+source/eglibc/+bug/956051
default[:lxc][:bugfix][:precise][:repo][:enabled] = false
default[:lxc][:bugfix][:precise][:repo][:path] = '/usr/src/precise_lts_apt'
default[:lxc][:bugfix][:precise][:repo][:exec] = '/usr/local/bin/precise_lts_apt_lxc'
default[:lxc][:bugfix][:precise][:repo][:auto_enable_lwrp] = false
