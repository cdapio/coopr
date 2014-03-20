#
# Cookbook Name:: hadoop_wrapper
# Recipe:: default
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

# We require Java, and the Hadoop cookbook doesn't have it as a dependency
include_recipe 'java::default'

if node.has_key? 'java' and node['java'].has_key? 'java_home'

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

# HACK HACK HACK
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
