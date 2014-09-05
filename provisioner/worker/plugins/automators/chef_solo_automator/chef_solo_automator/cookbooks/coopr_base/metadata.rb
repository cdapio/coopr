name             "coopr_base"
maintainer       "Cask Data, Inc."
maintainer_email "ops@continuuity.com"
license          "Apache 2.0"
description      "Base settings for all Coopr hosts"
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.0'

depends "coopr_hosts"
depends "coopr_firewall"

depends "apt"
depends "ulimit"
