#
# Cookbook Name:: hadoop_wrapper
# Recipe:: hive_metastore_db_init
#
# Copyright (C) 2014 Continuuity, Inc.
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

# Set up our database
if node['hive'].key?('hive_site') && node['hive']['hive_site'].key?('javax.jdo.option.ConnectionURL')
  jdo_array = node['hive']['hive_site']['javax.jdo.option.ConnectionURL'].split(':')
  hive_uris = node['hive']['hive_site']['hive.metastore.uris'].gsub('thrift://', '').gsub(':9083', '').split(',')
  db_type = jdo_array[1]
  db_name = jdo_array[2].split('/').last.split('?').first
  db_user =
    if node['hive'].key?('hive_site') && node['hive']['hive_site'].key?('javax.jdo.option.ConnectionUserName')
      node['hive']['hive_site']['javax.jdo.option.ConnectionUserName']
    end
  db_pass =
    if node['hive'].key?('hive_site') && node['hive']['hive_site'].key?('javax.jdo.option.ConnectionPassword')
      node['hive']['hive_site']['javax.jdo.option.ConnectionPassword']
    end
  sql_dir = '/usr/lib/hive/scripts/metastore/upgrade'

  case db_type
  when 'mysql'
    include_recipe 'database::mysql'
    mysql_connection_info = {
      :host     => 'localhost',
      :username => 'root',
      :password => node['mysql']['server_root_password']
    }
    mysql_database db_name do
      connection mysql_connection_info
      action :create
    end
    mysql_database_user db_user do
      connection mysql_connection_info
      password db_pass
      action :create
    end
    mysql_database 'import-hive-schema' do
      connection mysql_connection_info
      database_name db_name
      sql lazy { ::File.open(Dir.glob("#{sql_dir}/mysql/hive-schema-*").sort_by! { |s| s[/\d+/].to_i }.last).read }
      action :query
    end
    hive_uris.each do |hive_host|
      mysql_database_user "#{db_user}-#{hive_host}" do
        connection mysql_connection_info
        username db_user
        database_name db_name
        password db_pass
        host hive_host
        privileges ['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'LOCK TABLES', 'EXECUTE']
        action :grant
      end
    end

  when 'postgresql'
    include_recipe 'database::postgresql'
    postgresql_connection_info = {
      :host     => '127.0.0.1',
      :port     => node['postgresql']['config']['port'],
      :username => 'postgres',
      :password => node['postgresql']['password']['postgres']
    }
    postgresql_database db_name do
      connection postgresql_connection_info
      action :create
    end
    postgresql_database_user db_user do
      connection postgresql_connection_info
      password db_pass
      action :create
    end
    postgresql_database 'import-hive-schema' do
      connection postgresql_connection_info
      database_name db_name
      sql lazy { ::File.open(Dir.glob("#{sql_dir}/postgres/hive-schema-*").sort_by! { |s| s[/\d+/].to_i }.last).read }
      action :query
    end
    hive_uris.each do |hive_host|
      postgresql_database_user "#{db_user}-#{hive_host}" do
        connection postgresql_connection_info
        username db_user
        database_name db_name
        password db_pass
        host hive_host
#        privileges %w(SELECT INSERT UPDATE DELETE)
        privileges [:all]
        action :grant
      end
    end
  else
    Chef::Log.info('Only MySQL and PostgreSQL are supported for automatically creating users and databases')
  end
end
