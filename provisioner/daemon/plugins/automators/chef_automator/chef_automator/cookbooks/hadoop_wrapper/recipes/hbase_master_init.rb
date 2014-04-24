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

dfs = node['hadoop']['core_site']['fs.defaultFS']

if node['hbase']['hbase_site']['hbase.rootdir'] =~ %r{^/|^hdfs://} && node['hbase']['hbase_site']['hbase.cluster.distributed'].to_s == 'true'
  # bootstrap HDFS for HBase
  ruby_block 'initaction-create-hbase-hdfs-rootdir' do
    block do
      resources('execute[hbase-hdfs-rootdir]').run_action(:run)
    end
    not_if "hdfs dfs -test -d #{node['hbase']['hbase_site']['hbase.rootdir']}", :user => 'hdfs'
  end
end

if node['hbase']['hbase_site'].key?('hbase.bulkload.staging.dir')
  ruby_block 'initaction-create-hbase-bulkload-stagingdir' do
    block do
      resources('execute[hbase-bulkload-stagingdir]').run_action(:run)
    end
    not_if "hdfs dfs -test -d #{node['hbase']['hbase_site']['hbase.bulkload.staging.dir']}", :user => 'hdfs'
  end
end
