# sysctl [![Build Status](https://travis-ci.org/onehealth-cookbooks/sysctl.png?branch=master)](https://travis-ci.org/onehealth-cookbooks/sysctl)

Description
===========

Set [sysctl](http://en.wikipedia.org/wiki/Sysctl) system control parameters via Chef


Platforms
=========

* Debian/Ubuntu
* RHEL/CentOS
* Scientific Linux
* PLD Linux (not tested)

Usage
=======

There are two main ways to interact with the cookbook. This is via chef [attributes](http://docs.opscode.com/essentials_cookbook_attribute_files.html) or via the provided [LWRP](http://docs.opscode.com/lwrp.html).

# Cookbook Attributes

* `node['sysctl']['params']` - A namespace for setting sysctl parameters.
* `node['sysctl']['conf_dir']` - Specifies the sysctl.d directory to be used. Defaults to `/etc/sysctl.d` on the Debian and RHEL platform families, otherwise `nil`
* `node['sysctl']['allow_sysctl_conf']` - Defaults to false.  Using `conf_dir` is highly recommended. On some platforms that is not supported. For those platforms, set this to `true` and the cookbook will rewrite the `/etc/sysctl.conf` file directly with the params provided. Be sure to save any local edits of `/etc/sysctl.conf` before enabling this to avoid losing them.

Note: if `node['sysctl']['conf_dir']` is set to nil and `node['sysctl']['allow_sysctl_conf']` is not set, no config will be written

# Setting Sysctl Parameters

## Using Attributes

Setting variables in the `node['sysctl']['params']` hash will allow you to easily set common kernel parameters across a lot of nodes.
All you need to do to have them loaded is to include `sysctl::apply` anywhere in your run list of the node. It is recommended to do this early in the run list, so any recipe that gets applied afterwards that may depend on the set parameters will find them to be set.

The attributes method is easiest to implement if you manage the kernel parameters at the system level opposed to a per cookbook level approach.
The configuration will be written out when `sysctl::apply` gets run, which allows the parameters set to be persisted during a reboot.

### Examples

Set vm.swapiness to 20 via attributes

```` ruby
    node.default['sysctl']['params']['vm']['swappiness'] = 20

    include_recipe 'sysctl::apply'
````

## Using LWRPs

The `sysctl_param` LWRP can be called from wrapper and application cookbooks to immediately set the kernel parameter and cue the kernel parameter to be written out to the configuration file.

### sysctl_param

Actions

- apply (default)
- remove
- nothing

Attributes

- key
- value

### Examples

Set vm.swapiness to 20 via sysctl_param LWRP

```` ruby
    sysctl_param 'vm.swappiness' do
      value 20
    end
````
Remove sysctl parameter and set net.ipv4.tcp_fin_timeout back to default

```` ruby
    sysctl_param 'net.ipv4.tcp_fin_timeout' do
      value 30
      action :remove
    end
````

# Reading Sysctl Parameters

## Ohai Plugin

The cookbook also includes an Ohai 7 plugin that can be installed by adding `sysctl::ohai_plugin` to your run_list. This will populate `node['sys']` with automatic attributes that mirror the layout of `/proc/sys`.

To see ohai plugin output manually, you can run `ohai -d /etc/chef/ohai_plugins sys` on the command line.

# Development

We have written unit tests using [chefspec](http://code.sethvargo.com/chefspec/) and integration tests in [serverspec](http://serverspec.org/) executed via [test-kitchen](http://kitchen.ci).
Much of the tooling around this cookbook is exposed via guard and test kitchen, so it is highly recommended to learn more about those tools.

## Vagrant Plugin Dependencies

The integration tests can be run via test-kitchen using vagrant, but it depends on the following vagrant plugins:

```
vagrant plugin install vagrant-omnibus
```

Tested with 
* Vagrant (version 1.6.1)
* vagrant-omnibus (1.4.1)

## Running tests

The following commands will run the tests:

```
bundle install
bundle exec rubocop
bundle exec foodcritic .
bundle exec rspec
bundle exec kitchen test default-ubuntu-1404
bundle exec kitchen test default-centos-65
```

The above will do ruby style ([rubocop](https://github.com/bbatsov/rubocop)) and cookbook style ([foodcritic](http://www.foodcritic.io/)) checks followed rspec unit tests ensuring proper cookbook operation.Integration tests will be run next on two separate linux platforms (Ubuntu 14.04 LTS Precise 64-bit and CentOS 6.5). Please run the tests on any pull requests that you are about to submit and write tests for defects or new features to ensure backwards compatibility and a stable cookbook that we can all rely upon.

## Running tests continuously with guard

This cookbook is also setup to run the checks while you work via the [guard gem](http://guardgem.org/).

```
bundle install
bundle exec guard start
```

## ChefSpec LWRP Matchers

The cookbook exposes a chefspec matcher to be used by wrapper cookbooks to test the cookbooks LWRP. See `library/matchers.rb` for basic usage.

# Links

There are a lot of different documents that talk about system control parameters, the hope here is to point to some of the most useful ones to provide more guidance as to what the possible kernel parameters are and what they mean.

* [Linux Kernel Sysctl](https://www.kernel.org/doc/Documentation/sysctl/)
* [Linux Kernel IP Sysctl](http://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt)
* [THE /proc FILESYSTEM (Jun 2009)](http://www.kernel.org/doc/Documentation/filesystems/proc.txt)
* [RHEL 5 VM/Page Cache Tuning Presentation (2009) pdf](http://people.redhat.com/dshaks/Larry_Shak_Perf_Summit1_2009_final.pdf)
* [Arch Linux SysCtl Tutorial (Feb 2013)](http://gotux.net/arch-linux/sysctl-config/)
* [Old RedHat System Tuning Overview (2001!)](http://people.redhat.com/alikins/system_tuning.html)
* [Tuning TCP For The Web at Velocity 2013 (video)](http://vimeo.com/70369211), [slides](http://cdn.oreillystatic.com/en/assets/1/event/94/Tuning%20TCP%20For%20The%20Web%20Presentation.pdf)
* [Adventures in Linux TCP Tuning (Nov 2013)](http://thesimplecomputer.info/adventures-in-linux-tcp-tuning-page2/)
* [Part 1: Lessons learned tuning TCP and Nginx in EC2 (Jan 2014)](http://engineering.chartbeat.com/2014/01/02/part-1-lessons-learned-tuning-tcp-and-nginx-in-ec2/)
