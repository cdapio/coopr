#
# Cookbook Name:: modules
# Provider:: modules_multi
# Author:: Guilhem Lettron <guilhem.lettron@youscribe.com>
#
# Copyright 20012, Societe Publica.
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

use_inline_resources

include Chef::DSL::IncludeRecipe

def path
  new_resource.path ? new_resource.path : "/etc/modules-load.d/#{new_resource.name}.conf"
end

def serializeOptions
  if new_resource.options
    output = ""
    new_resource.options.each do |option, value|
      output << " " + option + "=" + value
    end
    return output
  end
end

action :save do

  include_recipe "modules::config"

  template path do
    source "modules.conf.erb"
    owner "root"
    group "root"
    mode "0644"
    variables(
      :modules => new_resource.modules
      )
    notifies :start, "service[modules-load]"
    only_if { supported? }
  end
end

#action :load do
#  new_resource.modules.each do |module|
#    execute "load module" do
#      command "modprobe #{name} #{serializeOptions}"
#    end
#  end
#end

action :remove do
  file path do
    action :delete
  end
  #TODO test this function
  new_resource.modules.each do |name|
    execute "unload module" do
      command "modprobe -r #{name}"
    end
  end
end

