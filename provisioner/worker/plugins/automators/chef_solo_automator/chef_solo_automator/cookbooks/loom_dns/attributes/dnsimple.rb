# We need to build early
default['build_essential']['compiletime'] = true
default['build-essential']['compiletime'] = true
# Specify a fog version
default['dnsimple']['fog_version'] = '1.21.0'
# Our configuration
default['loom_dns']['dnsimple']['use_databag'] = true
default['loom_dns']['dnsimple']['databag_name'] = 'creds'
default['loom_dns']['dnsimple']['databag_item'] = 'dnsimple'
# The following attributes can be used in place of a data bag
default['loom_dns']['dnsimple']['username'] = nil
default['loom_dns']['dnsimple']['password'] = nil
