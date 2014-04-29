#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hbase_master_init
#
# Copyright (C) 2013-2014 Continuuity, Inc.
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

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::hbase_master'

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
