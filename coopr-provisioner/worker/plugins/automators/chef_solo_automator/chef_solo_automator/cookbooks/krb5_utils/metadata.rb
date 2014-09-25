name             'krb5_utils'
maintainer       'Cask Data, Inc.'
maintainer_email 'ops@cask.co'
license          'Apache License, Version 2.0'
description      'Set of utility resources which can be used to setup Kerberos'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version          '0.1.1'

depends 'krb5'
depends 'yum-epel'
