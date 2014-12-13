name             'coopr_base'
maintainer       'Cask'
maintainer_email 'ops@cask.co'
license          'Apache 2.0'
description      'Base settings for all hosts'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.1'

depends 'coopr_dns'
depends 'coopr_firewall'
depends 'coopr_hosts'

depends 'apt'
depends 'yum-epel'
depends 'ulimit'

depends 'chef-solo-search' 
depends 'sudo'
depends 'users'
