#
# Cookbook Name:: hadoop_wrapper
# Recipe:: kerberos_init
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

# Enable kerberos security
if node['hadoop'].key?('core_site') && node['hadoop']['core_site'].key?('hadoop.security.authorization') &&
  node['hadoop']['core_site'].key?('hadoop.security.authentication') &&
  node['hadoop']['core_site']['hadoop.security.authorization'].to_s == 'true' &&
  node['hadoop']['core_site']['hadoop.security.authentication'] == 'kerberos'

  include_recipe 'krb5'
  Chef::Log.info("Secure Hadoop Enabled: Kerberos Realm '#{node['krb5']['krb5_conf']['realms']['default_realm']}'")
  secure_hadoop_enabled = true

  # Create users for services not in base Hadoop
  %w(hbase zookeeper).each do |u|
    user u do
      action :create
    end
  end

  # Create service keytabs for all services, since we may be a client
  node.default['krb5_utils']['krb5_service_keytabs'] = {
    'HTTP' => { 'owner' => 'hdfs', 'group' => 'hadoop', 'mode' => '0640' },
    'hdfs' => { 'owner' => 'hdfs', 'group' => 'hadoop', 'mode' => '0640' },
    'hbase' => { 'owner' => 'hbase', 'group' => 'hadoop', 'mode' => '0640' },
    'mapred' => { 'owner' => 'mapred', 'group' => 'hadoop', 'mode' => '0640' },
    'yarn' => { 'owner' => 'yarn', 'group' => 'hadoop', 'mode' => '0640' },
    'zookeeper' => { 'owner' => 'zookeeper', 'group' => 'hadoop', 'mode' => '0640' }
  }
  node.default['krb5_utils']['krb5_user_keytabs'] = {
    'yarn' => { 'owner' => 'yarn', 'group' => 'hadoop', 'mode' => '0640' }
  }
  include_recipe 'krb5_utils'
  if node['java']['install_flavor'] == 'oracle'
    include_recipe 'hadoop_wrapper::jce'
  end
  # Hack up /etc/default/hadoop-hdfs-datanode
  execute 'modify-etc-default-files' do
    command 'sed -i -e "/HADOOP_SECURE_DN/ s/^#//g" /etc/default/hadoop-hdfs-datanode'
    only_if 'test -e /etc/default/hadoop-hdfs-datanode'
  end
  # We need to kinit as hdfs to create directories
  execute 'kinit-as-hdfs-user' do
    command "kinit -kt #{node['krb5_utils']['keytabs_dir']}/hdfs.service.keytab hdfs/#{node['fqdn']}@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
    user 'hdfs'
    group 'hdfs'
    only_if { File.exist?("#{node['krb5_utils']['keytabs_dir']}/hdfs.service.keytab") }
  end
end

if node['hbase'].key?('hbase_site') && node['hbase']['hbase_site'].key?('hbase.security.authorization') &&
  node['hbase']['hbase_site'].key?('hbase.security.authentication') &&
  node['hbase']['hbase_site']['hbase.security.authorization'].to_s == 'true' &&
  node['hbase']['hbase_site']['hbase.security.authentication'] == 'kerberos'

  if secure_hadoop_enabled.nil?
    Chef::Application.fatal!('You must enable kerberos in Hadoop or disable kerberos for HBase!')
  end
end
