#
# Cookbook Name:: krb5_utils
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

# node['java']['jdk_version']
# download_direct_from_oracle(tarball_name, new_resource)

include_recipe 'java'

if node['java'].has_key 'jdk_version'
  case node['java']['jdk_version']
  when "6"
    jdk => {
      url => "http://download.oracle.com/otn-pub/java/jce_policy/6/jce_policy-6.zip"
      checksum => "d0c2258c3364120b4dbf7dd1655c967eee7057ac6ae6334b5ea8ceb8bafb9262"
    }
  when "7"
    jdk => {
      url => "http://download.oracle.com/otn-pub/java/jce/7/UnlimitedJCEPolicyJDK7.zip"
      checksum => "7a8d790e7bd9c2f82a83baddfae765797a4a56ea603c9150c87b7cdb7800194d"
    }
  end
  download_direct_from_oracle "jce#{node['java']['jdk_version']}.zip" jdk
end
