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

dfs = node['hadoop']['core_site']['fs.defaultFS']

ruby_block 'initaction-create-yarn-hdfs-tmpdir' do
  block do
    resources('execute[yarn-hdfs-tmpdir').run_action(:run)
  end
  not_if "hdfs dfs -test -d #{dfs}/tmp", :user => 'hdfs'
end
