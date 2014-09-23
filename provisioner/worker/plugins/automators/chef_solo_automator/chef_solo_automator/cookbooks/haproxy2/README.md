# Description

Installs and configures HAproxy

# Requirements

## Chef

Tested on 0.10.8 but newer and older version should work just fine.

## Platform

The following platforms have been tested with this cookbook, meaning that the
recipes run on these platforms without error:

* ubuntu
* debian

## Cookbooks

There are **no** external cookbook dependencies. 

# Installation

Just place the haproxy2 directory in your chef cookbook directory and
upload it to your Chef server.

# Usage

Simply include `recipe[haproxy2]` in your run_list.

This cookbook does not have a template to create the haproxy.cfg file, 
this file is generated dinamically depending of the pairs of keys and values 
of the attributes.

The global and the defaults sections, the listens, the frontends and the 
backends follow the same rules regarding how the cookbook will generate 
the configuration file. The idea is generate the config file setting attributes
in the node, or trhough roles avoiding the templates.

The global and defaults sections are hashes with which their pair of keys and 
values are the settings that will applied on them.

The listens, frontends and the backends are an array of hashes, where each 
pair of keys and values of each hash are the settings that will be applied 
for them. They have three special attributes (name, role_app and
member_options) that will not be considered settings of the listen, 
frontend or the backend but they are used for:

* **name**: defines the name of the listen, frontend or backend
* **role_app**: defines the role with which the nodes will be searched in order
  to use their info to set the servers options. 
* **member_options**: It is a  hash that could contain the keys `port` (it is the 
  port that will be used in the server option) and `extra` (you can set the extra 
  options for the server option). 

Regarding the values of the attributes, if you need to set a parameter 
without values, you need to set this value to `true`, but if you need to set
a value you need to set a string. If it is `false`, the parameter will be not
set. If it is an array, the parameter will be set with the elements of the 
array individually.

There is three special keywords (IPADDRESS, HOSTNAME, FQDN) that will be
replaced by the corresponding attribute of the node.

For example, the following settings:

    "haproxy" => {
      "global" => {
        "log" => "/dev/log local0 notice",
        "maxconn" => 4096,
        "debug" => false,
        "quiet" => true,
        "user" => "haproxy",
        "group" => "haproxy"
      },
      "defaults" => {
        "log" => "global",
        "mode" => "http",
        "option" => [ "httplog", "dontlognull", "redispatch" ],
        "retries" =>  3,
        "maxconn" => 2000,
        "contimeout" => 5000,
        "clitimeout" => 50000,
        "srvtimeout" => 50000
      },
      "listen" => [
        {
          "name" => "load_balancer",
          "role_app" => lb_nodes",
          "member_options" => {
            "port" => "1060",
            "extra" => "check"
          },
          "bind" => "IPADDRESS:80",
          "maxconn" => "500000",
          "balance" => "url_param jid",
          "option" => ["httpclose", "redispatch", "forwardfor"]
        },
        {
          "name" => "health_check",
          "bind" => "127.0.0.1:60000",
          "mode" => "health"
        },
        {
          "name" => "stats",
          "bind" => "10.10.10.10:80",
          "maxconn" => "10",
          "mode" => "http",
          "stats" => [
            "enable",
            "hide-version",
            "realm Haproxy",
            "uri /proxy?stats",
            "auth status:pass123"
          ]
        },
        {
          "name" => "admin",
          "bind" => "0.0.0.0:22002",
          "mode" => "http",
          "stats" => "uri /"
        }
      ]
    }

Will generate the following configuration file (the ip address of the node will be 40.51.127.1):

    global
           group haproxy
           log /dev/log local0 notice
           maxconn 4096
           user haproxy
           quiet
    
    defaults
           clitimeout 50000
           contimeout 5000
           log global
           maxconn 2000
           mode http
           option httplog
           option dontlognull
           option redispatch
           retries 3
           srvtimeout 50000

    listen load_balancer
           balance url_param jid
           bind 40.51.127.1:80
           maxconn 500000
           option httpclose
           option redispatch
           option forwardfor
           server node00 10.0.0.100:1060 check
           server node01 10.0.0.101:1060 check
           server node02 10.0.0.102:1060 check
           server node03 10.0.0.103:1060 check

    listen health_check
           bind 127.0.0.1:60000
           mode health

    listen stats
           bind 10.10.10.10:80
           maxconn 10
           mode http
           stats enable
           stats hide-version
           stats realm Haproxy
           stats uri /proxy?stats
           stats auth status:pass123

    listen admin
           bind 0.0.0.0:22002
           mode http
           stats uri /


(The servers option of the load_balancer listen were added automatically. The cookbook search
the nodes that have the role lb_nodes in their runlist and added them to the listen settings)

# Recipes

## default

This recipe installs, configures and starts the haproxy service.

# Attributes

## `node['haproxy']['daemon']['enable']`

If it true, set ENABLE=1 in the /etc/default/haproxy file.

## `node['haproxy']['daemon']['extra_options']`

It set the extra options which will be started the haproxy daemon.

## `node['haproxy']['global']`

Hash that contains the options and the values of the global section.

## `node['haproxy']['defaults']`

Hash that contains the options and the values of the defaults section.

## `node['haproxy']['frontend']`

Array of the hashes. Each hash contain the options and the values of a
frontend definition.

## `node['haproxy']['backend']`

Array of the hashes. Each hash contain the options and the values of a
backend definition.

## `node['haproxy']['listen']`

Array of the hashes. Each hash contain the options and the values of a
listen definition.

# Resources and Providers

There are **none** defined.

# Libraries

There are **none** defined.

# Development

* Source hosted at [GitHub][repo]
* Report issues/Questions/Feature requests on [GitHub Issues][issues]

Pull requests are very welcome! Make sure your patches are well tested.
Ideally create a topic branch for every separate change you make.

# License and Author

Author:: Claudio Cesar Sanchez Tejeda <demonccc@gmail.com>

Copyright:: 2012, Claudio Cesar Sanchez Tejeda

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[repo]:         https://github.com/demonccc/chef-repo
[issues]:       https://github.com/demonccc/chef-repo/issues
