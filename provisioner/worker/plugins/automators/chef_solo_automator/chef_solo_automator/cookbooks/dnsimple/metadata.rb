name             'dnsimple'
maintainer       'DNSimple'
maintainer_email 'ops@dnsimple.com'
license          'Apache 2.0'
description      'Installs/Configures dnsimple'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version          '1.0.0'

recipe   'dnsimple', 'Installs fog gem to use w/ the dnsimple_record'

supports 'amazon'
supports 'centos'
supports 'debian'
supports 'fedora'
supports 'redhat'
supports 'rhel'
supports 'ubuntu'

depends 'build-essential', '~> 1.4.2'
