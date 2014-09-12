name             "coopr_base"
maintainer       "Cask"
maintainer_email "ops@cask.co"
license          "Apache 2.0"
description      "Base settings for all hosts"
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.0'

depends "coopr_hosts"
depends "coopr_firewall"

depends "apt"
depends "ulimit"
