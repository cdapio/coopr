## Requirements
   * Java 6 or Java 7
   * Node v0.10.26 or higher
   * Ruby 1.9.0p0 or higher
   * XCode Command Line Developer Tools (Mac only)

## Build
```
git submodule init
git submodule update
mvn clean package assembly:single -DskipTests
```

## Install

Unpack Coopr Standalone ZIP onto your system and enter its directory.

Next, install the appropriate Ruby Gems on the system.
```
sudo gem install bundler
bundle install --gemfile provisioner/Gemfile
```

## Start
```
bin/coopr.sh start
```

   * Login at http://localhost:8100 with tenant `superadmin`, username `admin`, and password `admin`

## Stop
```
bin/coopr.sh stop
```

## Create a cluster
Once Cask Coopr is running, follow the instructions in the quickstart guide at 
http://docs.cask.co/coopr/current/en/guide/quickstart/index.html#getting-started.

It contains step by step instructions for creating a Hadoop cluster with different providers.
