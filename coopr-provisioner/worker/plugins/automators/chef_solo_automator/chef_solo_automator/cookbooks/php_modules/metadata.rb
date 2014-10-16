name             'php_modules'
maintainer       'Cask Data, Inc'
maintainer_email 'ops@cask.co'
license          'Apache 2.0'
description      'Installs PHP modules using the php_pear LWRP'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.1'

depends 'apache'
depends 'php'
depends 'build-essential'
