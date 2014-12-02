name             'lxc'
maintainer       'Chris Roberts'
maintainer_email 'chris@hw-ops.com'
license          'Apache 2.0'
description      'Chef driven Linux Containers'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version          '2.0.0'

suggests 'omnibus_updater'
suggests 'bridger'

depends 'dpkg_autostart', '>= 0.1.10'
depends 'polipo'
depends 'iptables-ng'
