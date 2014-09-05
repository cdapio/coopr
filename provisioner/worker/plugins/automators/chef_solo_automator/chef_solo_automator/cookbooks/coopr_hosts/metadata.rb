name             "coopr_hosts"
maintainer       "Cask Data, Inc."
maintainer_email "ops@continuuity.com"
license          "Apache 2.0"
description      "Installs/Configures /etc/hosts using data from Coopr"
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.0'

depends "hostsfile"
