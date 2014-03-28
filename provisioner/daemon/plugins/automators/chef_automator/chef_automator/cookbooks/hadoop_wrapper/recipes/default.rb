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

# Enable kerberos security
if (node['hadoop'].has_key? 'core_site' and node['hadoop']['core_site'].has_key? 'hadoop.security.authorization' and
  node['hadoop']['core_site'].has_key? 'hadoop.security.authentication' and
  node['hadoop']['core_site']['hadoop.security.authorization'] == 'true' and
  node['hadoop']['core_site']['hadoop.security.authentication'].downcase == 'kerberos')

    node.default['krb5_utils']['krb5_service_keytabs'] = {
      "HTTP" => { "owner" => "hdfs", "group" => "hadoop", "mode" => "0640" },
      "hdfs" => { "owner" => "hdfs", "group" => "hadoop", "mode" => "0640" },
      "hbase" => { "owner" => "hbase", "group" => "hadoop", "mode" => "0640" },
      "yarn" => { "owner" => "yarn", "group" => "hadoop", "mode" => "0640" },
      "zookeeper" => { "owner" => "zookeeper", "group" => "hadoop", "mode" => "0640" }
    }

    include_recipe 'krb5'
    include_recipe 'krb5_utils'
  end
end
