#
# Cookbook Name:: loom_base
# Recipe:: default
#
# Copyright © 2013 Cask Data, Inc.
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

# This forces an apt-get update on Ubuntu/Debian
case node['platform_family']
when 'debian'
  include_recipe "apt::default"
end

# We always run our hosts and firewall cookbooks
include_recipe "loom_firewall::default"
include_recipe "loom_hosts::default"

# ensure user ulimits are enabled 
include_recipe "ulimit::default"
