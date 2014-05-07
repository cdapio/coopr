# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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
require 'chef/knife/openstack_base'
require 'chef/knife/openstack_server_create'

class Chef
  class Knife
    class LoomOpenstackServerCreate < OpenstackServerCreate

      Chef::Knife::OpenstackServerCreate.load_deps

      # support options provided by openstack base module
      include Knife::OpenstackBase

      # quick workaround for class level instance vars 
      self.options.merge!(OpenstackServerCreate.options)

      # load dep 
      deps do
        require 'fog'
        require 'readline'
        require 'chef/json_compat'
        require 'chef/knife/bootstrap'
        Chef::Knife::Bootstrap.load_deps
      end

      banner 'knife loom openstack server create (options)'

      def run

        $stdout.sync = true

        validate!

        # Maybe deprecate this option at some point
        config[:bootstrap_network] = 'private' if config[:private_network]

        unless Chef::Config[:knife][:image]
          ui.error("You have not provided a valid image value.  Please note the short option for this value recently changed from '-i' to '-I'.")
          exit 1
        end

        if locate_config_value(:bootstrap_protocol) == 'winrm'
          load_winrm_deps
        end

        node_name = get_node_name(config[:chef_node_name] || config[:server_name])

        server_def = {
          :name => node_name,
          :image_ref => locate_config_value(:image),
          :flavor_ref => locate_config_value(:flavor),
          :security_groups => locate_config_value(:security_groups),
          :availability_zone => locate_config_value(:availability_zone),
          :key_name => locate_config_value(:openstack_ssh_key_id)
        }

        Chef::Log.debug("Name #{node_name}")
        Chef::Log.debug("Image #{locate_config_value(:image)}")
        Chef::Log.debug("Flavor #{locate_config_value(:flavor)}")
        Chef::Log.debug("Availability zone #{locate_config_value(:availability_zone)}")
        Chef::Log.debug("Requested Floating IP #{locate_config_value(:floating_ip)}")
        Chef::Log.debug("Security Groups #{locate_config_value(:security_groups)}")
        Chef::Log.debug("Creating server #{server_def}")

        server = connection.servers.new(server_def)

        server.save

        msg_pair("Name", server.name)
        msg_pair("Instance ID", server.id)
        msg_pair("Availability zone", server.availability_zone) if server.availability_zone

        msg_pair("SSH Identity File", config[:identity_file])
        msg_pair("SSH Keypair", server.key_name) if server.key_name
        msg_pair("SSH Password", server.password) if (server.password && !server.key_name)

        puts "SERVERID: #{server.id}"
        if (server.password && !server.key_name)
          return { "status" => 0, "providerid" => server.id.to_s, "rootpassword" => server.password }
        else
          return { "status" => 0, "providerid" => server.id.to_s }
        end
      end
    end
  end
end

