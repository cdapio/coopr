#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hadoop_yarn_resourcemanager_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::hadoop_yarn_resourcemanager'

execute "initaction-create-hdfs-tmp" do
  not_if        "hdfs dfs -test -d /tmp", :user => "hdfs"
  command       "hdfs dfs -mkdir /tmp && hadoop fs -chmod -R 1777 /tmp"
  timeout 300
  user "hdfs"
  group "hdfs"
end


