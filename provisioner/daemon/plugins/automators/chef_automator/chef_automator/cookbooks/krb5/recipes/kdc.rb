#
# Cookbook Name:: krb5
# Recipe:: kdc
#
# Copyright 2014, Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include_recipe 'krb5::default'

node['krb5']['kdc']['packages'].each do |krb5_package|
  package krb5_package
end

case node['platform_family']
when 'rhel'
  kdc_dir = '/var/kerberos/krb5kdc'
  etc_dir = kdc_dir
  kdc_svc = 'krb5kdc'
when 'debian'
  etc_dir = '/etc/krb5kdc'
  kdc_svc = 'krb5-kdc'
end

template "#{etc_dir}/kdc.conf" do
  owner 'root'
  group 'root'
  mode '0644'
  variables node['krb5']['kdc_conf']
end

service 'krb5-kdc' do
  service_name kdc_svc
  action :start
end
