# Specify a fog version
default['dnsimple']['fog_version'] = '1.21.0'
# Our configuration
default['loom_dns']['dnsimple']['use_databag'] = true
default['loom_dns']['dnsimple']['databag_name'] = 'creds'
default['loom_dns']['dnsimple']['databag_item'] = 'dnsimple'
