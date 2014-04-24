Requirements
============
Java 6 or Java 7
Node v0.10.26 or higher
Ruby 1.9.0p0 or higher
XCode Command Line Developer Tools (Mac only)

Install
======
sudo gem install knife-joyent --version 0.4.8
sudo gem install knife-rackspace --version 0.9.1
sudo gem install knife-openstack --version 0.8.1

Start
=====
bin/loom.sh start
Login at http://localhost:8100 with username 'admin' and password 'admin'

Stop
=====
bin/loom.sh stop

Create a cluster
================
Once Continuuity Loom is running, follow the instructions in the quickstart guide at 
http://continuuity.com/docs/loom/0.9.6/en/guide/quickstart/index.html#getting-started.
It contains step by step instructions for creating a Hadoop cluster with different providers.

Known Issues
============
 * XCode Command Line Developer Tools 5.1 (Mar 2014) will cause compilation failure of the knife plugins
 * Continuuity Loom standalone invocations of knife plugins are not isolated from any existing chef environment settings.  Please ensure there are no active knife.rb files before running Loom standalone.
