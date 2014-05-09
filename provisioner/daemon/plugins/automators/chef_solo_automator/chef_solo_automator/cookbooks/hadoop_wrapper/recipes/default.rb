#
# Cookbook Name:: hadoop_wrapper
# Recipe:: default
#
# Copyright (C) 2013 Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# We require Java, and the Hadoop cookbook doesn't have it as a dependency
include_recipe 'java::default'

if node.key?('java') && node['java'].key?('java_home')

  Chef::Log.info("JAVA_HOME = #{node['java']['java_home']}")

  # set in ruby environment for commands like hdfs namenode -format
  ENV['JAVA_HOME'] = node['java']['java_home']
  # set in hadoop_env
  node.default['hadoop']['hadoop_env']['java_home'] = node['java']['java_home']
  # set in hbase_env
  node.default['hbase']['hbase_env']['java_home'] = node['java']['java_home']
  # set in hive_env
  node.default['hive']['hive_env']['java_home'] = node['java']['java_home']
end

include_recipe 'hadoop::default'

# HBase needs snappy
pkg =
  case node['platform_family']
  when 'debian'
    'libsnappy1'
  when 'rhel'
    'snappy'
  end
package pkg do
  action :install
end

include_recipe 'hadoop_wrapper::kerberos_init'
