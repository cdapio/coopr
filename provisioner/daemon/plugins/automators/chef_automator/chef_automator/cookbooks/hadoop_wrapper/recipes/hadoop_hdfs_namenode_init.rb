#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hadoop_hdfs_namenode_init
#
# Copyright 2013, Continuuity
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'hadoop_wrapper::default'
include_recipe 'hadoop::hadoop_hdfs_namenode'

### TODO: due to a bug in the underlying hadoop cookbook where hadoop.tmp.dir can contain '${user}', for now
###         we handle the idempotency check in the ruby block itself
marker_file = "#{Chef::Config[:file_cache_path]}/hadoop_wrapper.nnformatted"
ruby_block 'initaction-format-namenode' do
  block do
    resources(:execute => 'hdfs-namenode-format').run_action(:run)
    File.open(marker_file, 'w') {}
  end
  not_if { File.exist? marker_file }
  ### TODO: this should check all dfs name dirs, not just the first
  # only_if { (Dir.entries("#{node['hadoop']['hdfs_site']['dfs.name.dir'].split(',').first}") - %w{ . .. }).empty? }
end
