#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hbase_master_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::hbase_master'

if node['hbase']['hbase_site']['hbase.rootdir'] =~ /^\/|^hdfs:\/\//i
  # bootstrap hdfs for hbase
  execute "initaction-create-hdfs-rootdir" do
    not_if        "hadoop fs -test -d #{node['hbase']['hbase_site']['hbase.rootdir']}", :user => "hbase"
    command       "hadoop fs -mkdir #{node['hbase']['hbase_site']['hbase.rootdir']} && hadoop fs -chown hbase #{node['hbase']['hbase_site']['hbase.rootdir']}"
    timeout 300
    user "hdfs"
    group "hdfs"
  end
end


