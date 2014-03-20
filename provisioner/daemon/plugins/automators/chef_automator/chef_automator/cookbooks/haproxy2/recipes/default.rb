#
# Author:: Claudio Cesar Sanchez Tejeda <demonccc@gmail.com>
# Cookbook Name:: haproxy2
# Recipe:: default
#
# Copyright 2012, Claudio Cesar Sanchez Tejeda
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

def generate_content(array) 
  content = ""
  array = array.sort
  array.each do |k,v|
    case v
    when Array
      v.each { |x| content += "        #{k} #{x}\n" }
    when TrueClass
      content += "        #{k}\n"
    when FalseClass
      content += ""
    when Fixnum
      content += "        #{k} #{v}\n"
    else
      unless k.eql?("name") or k.eql?("role_app") or k.eql?("member_options")
        [ "IPADDRESS", "HOSTNAME", "FQDN" ].each do |att|
          if v.include?(att)
            v.gsub!(att, node[att.downcase])
          end
        end
        content += "        #{k} #{v}\n"
      end
    end
  end
  return content
end

package "haproxy" do
  action :install
end

template "/etc/default/haproxy" do
  source "haproxy-default.erb"
  owner "root"
  group "root"
  mode 0644
  notifies :restart, "service[haproxy]"
end

service "haproxy" do
  supports :status => true, :restart => true, :reload => true
  action [ :enable, :start ]
end

haproxy_cfg_config = "# File managed by Chef\n# Don't edit it manually!\n\n"

haproxy_cfg_config += "global\n"
haproxy_cfg_config += generate_content(node['haproxy']['global'])

haproxy_cfg_config += "\ndefaults\n"
haproxy_cfg_config += generate_content(node['haproxy']['defaults'])


%w{ frontend backend listen }.each do |section|
  if node['haproxy'][section].is_a?(Array)
    node['haproxy'][section].each do |stanza|
      haproxy_cfg_config += "\n#{section} #{stanza['name']}\n"
    
      haproxy_cfg_config += generate_content(stanza)

      member_options = ""
    
      unless stanza['member_options'].nil?
        unless stanza['member_options']['port'].nil?
          member_options = ":#{stanza['member_options']['port']}"
        end
        unless stanza['member_options']['extra'].nil?
          member_options += " #{stanza['member_options']['extra']}" 
        end
      end
    
      unless stanza['role_app'].nil?  
        pool_members = search("node", "role:#{stanza['role_app']} AND chef_environment:#{node.chef_environment}") || []
        pool_members << node if node.run_list.roles.include?(stanza['role_app'])

        pool_members = pool_members.sort { |a,b| a[:hostname] <=> b[:hostname] }

        pool_members.map! do |member|
          server_ip = begin
            if member.attribute?('cloud')
              if node.attribute?('cloud') && (member['cloud']['provider'] == node['cloud']['provider'])
                member['cloud']['local_ipv4']
              else
                member['cloud']['public_ipv4']
              end
            else
              member['ipaddress']
            end
          end
          options = member_options
          [ "IPADDRESS", "HOSTNAME", "FQDN" ].each do |att|
            if member_options.include?(att)
              options = member_options.gsub(att, member[att.downcase])
            end
          end
          haproxy_cfg_config += "        server  #{member['hostname']} #{member['ipaddress']}#{options}\n"
        end
      end
    end
  end
end

file "/etc/haproxy/haproxy.cfg" do
  owner "root"
  group "root"
  mode 0644
  content  haproxy_cfg_config
  notifies :reload, "service[haproxy]"
end
