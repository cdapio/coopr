#
# Author:: Claudio Cesar Sanchez Tejeda <demonccc@gmail.com>
# Cookbook Name:: haproxy2
# Attributes:: default
#
# Author:: Claudio Cesar Sanchez Tejeda
#
# Copyright 2012, Claudio Cesar Sanchez Tejeda
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Daemon options

default['haproxy']['daemon']['enable'] = true
default['haproxy']['daemon']['extra_options'] = false
#default['haproxy']['daemon']['extra_options'] = "-de -m 16"

# Global section

default['haproxy']['global']['log'] = "/dev/log local0 notice"
default['haproxy']['global']['maxconn'] = 4096
default['haproxy']['global']['debug'] = false
default['haproxy']['global']['quiet'] = false
default['haproxy']['global']['user'] = "haproxy"
default['haproxy']['global']['group'] = "haproxy"

# Default section

default['haproxy']['defaults']['log'] = "global"
default['haproxy']['defaults']['mode'] = "http"
default['haproxy']['defaults']['option'] = [ "httplog", "dontlognull", "redispatch" ]
# For x_forwarded_for feature add httpclose and forwardfor to the options array.
#default['haproxy']['defaults']['option'] = [ "httplog", "dontlognull", "httpclose", "forwardfor" ]
default['haproxy']['defaults']['retries'] =  3
default['haproxy']['defaults']['maxconn'] = 2000
default['haproxy']['defaults']['contimeout'] = 5000
default['haproxy']['defaults']['clitimeout'] = 50000
default['haproxy']['defaults']['srvtimeout'] = 50000

# Frontends

default['haproxy']['frontend'] = false
#default['haproxy']['frontend'] = [
#  {
#    "name" => "http-in",
#    "bind" => "*:80",
#    "default_backend" => "servers"
#  }
#]

# Backends

default['haproxy']['backend'] = false
#default['haproxy']['frontend'] = [
#  {
#    "name" => "servers",
#    "role_app" => "backend_nodes",
#    "member_options" => {
#      "port" => "8000",
#      "extra" => "maxconn 32"
#    }
#  }
#]

# Listens

default['haproxy']['listen'] = [
  {
    "name" => "admin",
    "bind" => "0.0.0.0:22002",
    "mode" => "http",
    "stats" => "uri /"
  }
]
