#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hive_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::default'
include_recipe 'hadoop::hive'

dfs = node['hadoop']['core_site']['fs.defaultFS']

ruby_block 'initaction-create-hive-hdfs-homedir' do
  block do
    resources('execute[hive-hdfs-homedir').run_action(:run)
  end
  not_if "hdfs dfs -test -d #{dfs}/user/hive", :user => 'hdfs'
end
