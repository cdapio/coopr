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
#require 'chef/knife/rackspace_base'
require 'chef/knife/rackspace_server_create'

class Chef
  class Knife
    class LoomRackspaceServerCreate < RackspaceServerCreate

      # support options provided by rackspace base module
      include Knife::RackspaceBase

      # quick workaround for class level instance vars 
      self.options.merge!(RackspaceServerCreate.options)

      banner 'knife loom rackspace server create (options)'

      def run

        $stdout.sync = true

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
        networks = get_networks(Chef::Config[:knife][:rackspace_networks])

        rackconnect_wait = Chef::Config[:knife][:rackconnect_wait] || config[:rackconnect_wait]
        rackspace_servicelevel_wait = Chef::Config[:knife][:rackspace_servicelevel_wait] || config[:rackspace_servicelevel_wait]

        server = connection.servers.new(
          :name => node_name,
          :image_id => Chef::Config[:knife][:image],
          :flavor_id => locate_config_value(:flavor),
          :metadata => Chef::Config[:knife][:rackspace_metadata],
          :disk_config => Chef::Config[:knife][:rackspace_disk_config],
          :config_drive => locate_config_value(:rackspace_config_drive) || false,
          :personality => files,
          :keypair => Chef::Config[:knife][:rackspace_ssh_keypair]
        )

        if version_one?
          server.save
        else
          server.save(:networks => networks)
        end

        msg_pair("Instance ID", server.id)
        msg_pair("Host ID", server.host_id)
        msg_pair("Name", server.name)
        msg_pair("Flavor", server.flavor.name)
        msg_pair("Image", server.image.name)
        msg_pair("Metadata", server.metadata.all)
        msg_pair("RackConnect Wait", rackconnect_wait ? 'yes' : 'no')
        msg_pair("ServiceLevel Wait", rackspace_servicelevel_wait ? 'yes' : 'no')
        msg_pair("Password", server.password)
        msg_pair("SSH Key", Chef::Config[:knife][:rackspace_ssh_keypair])

        puts "SERVERID: #{server.id.to_s}"

        if (server.password && !server.key_name)
          return { "status" => 0, "providerid" => server.id.to_s, "rootpassword" => server.password }
        else
          return { "status" => 0, "providerid" => server.id.to_s }
        end
        #return { "status" => 0, "providerid" => server.id.to_s, "rootpassword" => server.password }
      end
    end
  end
end

