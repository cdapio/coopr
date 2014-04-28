#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hive_metastore_init
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

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::default'
include_recipe 'hadoop::hive_metastore'

dfs = node['hadoop']['core_site']['fs.defaultFS']

ruby_block 'initaction-create-hive-hdfs-warehousedir' do
  block do
    resources('execute[hive-hdfs-warehousedir]').run_action(:run)
  end
  not_if "hdfs dfs -test -d #{dfs}/#{node['hive']['hive_site']['hive.metastore.warehouse.dir']}", :user => 'hdfs'
end

scratch_dir =
  if node['hive'].key?('hive_site') && node['hive']['hive_site'].key?('hive.exec.scratchdir')
    node['hive']['hive_site']['hive.exec.scratchdir']
  else
    '/tmp/hive-${user.name}'
  end

unless scratch_dir == '/tmp/hive-${user.name}'
  ruby_block 'initaction-create-hive-hdfs-scratchdir' do
    block do
      resources('execute[hive-hdfs-scratchdir]').run_action(:run)
    end
    not_if "hdfs dfs -test -d #{dfs}/#{node['hive']['hive_site']['hive.exec.scratchdir']}", :user => 'hdfs'
  end
end
