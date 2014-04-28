#
# Cookbook Name:: hadoop_wrapper
# Recipe:: jce
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

include_recipe 'java'

if node['java'].key? 'jdk_version'
  case node['java']['jdk_version']
  when '6', 6
    tarball_url = 'http://download.oracle.com/otn-pub/java/jce_policy/6/jce_policy-6.zip'
    tarball_checksum = 'd0c2258c3364120b4dbf7dd1655c967eee7057ac6ae6334b5ea8ceb8bafb9262'
  when '7', 7
    tarball_url = 'http://download.oracle.com/otn-pub/java/jce/7/UnlimitedJCEPolicyJDK7.zip'
    tarball_checksum = '7a8d790e7bd9c2f82a83baddfae765797a4a56ea603c9150c87b7cdb7800194d'
  when '8', 8
    tarball_url = 'http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip'
    tarball_checksum = 'f3020a3922efd6626c2fff45695d527f34a8020e938a49292561f18ad1320b59'
  end

  package 'unzip' do
    action :install
  end
  package 'curl' do
    action :install
  end

  tarball_name = "jce#{node['java']['jdk_version']}.zip"
  download_path = "#{Chef::Config[:file_cache_path]}/#{tarball_name}"
  cookie = 'oraclelicense=accept-securebackup-cookie'

  bash 'download-jce-zipfile' do
    code "curl --create-dirs -L --cookie #{cookie} #{tarball_url} -o #{download_path}"
    not_if "echo '#{tarball_checksum}  #{download_path}' | sha256sum -c - >/dev/null"
  end

  jce_tmp = "/tmp/jce#{node['java']['jdk_version']}"

  bash 'unzip-jce-zipfile' do
    code <<-CODE
      mkdir -p #{jce_tmp}
      unzip -o #{download_path} -d #{jce_tmp}
    CODE
    not_if "test -e #{jce_tmp}/jce/US_export_policy.jar"
  end

  jce_dir = "#{node['java']['java_home']}/jre/lib/security"

  bash 'copy-jce-files' do
    code <<-CODE
      find -name '*.jar' -exec cp '{}' #{jce_dir} \\;
    CODE
    not_if { ::FileUtils.compare_file("#{jce_tmp}/jce/US_export_policy.jar", "#{jce_dir}/US_export_policy.jar") }
  end
end
