#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hive_metastore_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::default'
include_recipe 'hadoop::hive_metastore'

dfs = node['hadoop']['core_site']['fs.defaultFS']

ruby_block 'initaction-create-hive-hdfs-warehousedir' do
  block do
    resources('execute[hive-hdfs-warehousedir').run_action(:run)
  end
  not_if "hdfs dfs -test -d #{dfs}/#{node['hive']['hive_site']['hive.metastore.warehouse.dir']}", :user => 'hdfs'
end

ruby_block 'initaction-create-hive-hdfs-scratchdir' do
  block do
    resources('execute[hive-hdfs-scratchdir').run_action(:run)
  end
  not_if "hdfs dfs -test -d #{dfs}/#{node['hive']['hive_site']['hive.exec.scratchdir']}", :user => 'hdfs'
end
