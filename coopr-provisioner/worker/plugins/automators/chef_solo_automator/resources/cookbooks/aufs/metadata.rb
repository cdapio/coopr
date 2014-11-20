name              'aufs'
maintainer        'Brian Flad'
maintainer_email  'bflad417@gmail.com'
license           'Apache 2.0'
description       'Installs/Configures AUFS'
long_description  IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version           '0.1.1'
recipe            'aufs', 'Installs/Configures AUFS'
recipe            'aufs::module', 'Loads AUFS Linux module'
recipe            'aufs::package', 'Installs AUFS via package'

supports 'ubuntu', '>= 12.04'

depends 'apt'
depends 'modules'
