name             "loom_base"
maintainer       "Continuuity, Inc."
maintainer_email "ops@continuuity.com"
license          "Apache 2.0"
description      "Base settings for all Loom hosts"
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.0'

depends "loom_hosts"
depends "loom_firewall"

depends "apt"
depends "ulimit"
