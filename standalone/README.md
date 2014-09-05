Requirements
============
Java 6 or Java 7
Node v0.10.26 or higher
Ruby 1.9.0p0 or higher
XCode Command Line Developer Tools (Mac only)

Install
======
sudo gem install fog --version 1.21.0
sudo gem install sinatra --version 1.4.5
sudo gem install thin --version 1.6.2
sudo gem install rest_client --version 1.7.3

Start
=====
bin/coopr.sh start
Login at http://localhost:8100 with tenant 'superadmin', username 'admin', and password 'admin'

Stop
=====
bin/coopr.sh stop

Create a cluster
================
Once Coopr is running, follow the instructions in the quickstart guide at 
http://docs.cask.co/coopr/0.9.8/en/guide/quickstart/index.html#getting-started.

It contains step by step instructions for creating a Hadoop cluster with different providers.

