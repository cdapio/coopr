#
# Cookbook Name:: sysctl
# Recipe:: default
#
# Copyright 2011, Fewbytes Technologies LTD
# Copyright 2012, Chris Roberts <chrisroberts.code@gmail.com>
# Copyright 2013-2014, OneHealth Solutions, Inc.
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

include_recipe 'sysctl::service'

if node['sysctl']['conf_dir']
  directory node['sysctl']['conf_dir'] do
    owner 'root'
    group 'root'
    mode 0755
    action :create
  end
end

if Sysctl.config_file(node)
  # this is called by the sysctl_param lwrp to trigger template creation
  ruby_block 'save-sysctl-params' do
    action :nothing
    block do
    end
    notifies :create, "template[#{Sysctl.config_file(node)}]", :delayed
  end

  # this is called by the sysctl::apply recipe to trigger template creation
  ruby_block 'apply-sysctl-params' do
    action :nothing
    block do
    end
    notifies :create, "template[#{Sysctl.config_file(node)}]", :immediately
  end

  # this needs to have an action in case node.sysctl.params has changed
  # and also needs to be called for persistence on lwrp changes via the
  # ruby_block
  template Sysctl.config_file(node) do
    action :nothing
    source 'sysctl.conf.erb'
    mode '0644'
    notifies :start, 'service[procps]', :immediately
  end
end
