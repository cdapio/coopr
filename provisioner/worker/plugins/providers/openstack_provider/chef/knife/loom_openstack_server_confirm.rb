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
    class LoomOpenstackServerConfirm < OpenstackServerCreate

      Chef::Knife::OpenstackServerCreate.load_deps

      # support options provided by openstack base module
      include Knife::OpenstackBase

      # quick workaround for class level instance vars
      self.options.merge!(OpenstackServerCreate.options)

      # load deps
      deps do
        require 'fog'
        require 'readline'
        require 'chef/json_compat'
        require 'chef/knife/bootstrap'
        require 'ipaddr'
        Chef::Knife::Bootstrap.load_deps
      end

      banner 'knife loom openstack server confirm (options) SERVER-ID'

      def run
        $stdout.sync = true

        unless name_args.size === 1
          show_usage
          exit 1
        end
        
#       networks = get_networks(Chef::Config[:knife][:openstack_networks])

        id = name_args.first
        puts "fetching server for id: #{id}"
        server = self.connection.servers.get(id)

        Chef::Log.debug("Server state: #{server.state}")

        if (server.state == 'ERROR')
          ui.error("Server #{id} has failed")
          exit 1
        end

        # start copy from openstack server create

        # wait for it to be ready to do stuff
        print "\n#{ui.color("Waiting server", :magenta)}"

        server.wait_for(Integer(locate_config_value(:server_create_timeout))) { print "."; ready? }

        puts("\n")

        msg_pair("Name", server.name)
        msg_pair("Instance ID", server.id)
        msg_pair("Availability zone", server.availability_zone)
        msg_pair("Flavor", server.flavor['id'])
        msg_pair("Image", server.image['id'])
        msg_pair("SSH Identity File", config[:identity_file])
        msg_pair("SSH Keypair", server.key_name) if server.key_name
        msg_pair("SSH Password", server.password) if (server.password && !server.key_name)
        Chef::Log.debug("Addresses #{server.addresses}")

        msg_pair("Public IP Address", primary_public_ip_address(server.addresses)) if primary_public_ip_address(server.addresses)

        floating_address = locate_config_value(:floating_ip)
        Chef::Log.debug("Floating IP Address requested #{floating_address}")
        unless (floating_address == '-1') #no floating IP requested
          addresses = connection.addresses
          #floating requested without value
          if floating_address.nil?
            free_floating = addresses.find_index {|a| a.fixed_ip.nil?}
            if free_floating.nil? #no free floating IP found
              ui.error("Unable to assign a Floating IP from allocated IPs.")
              exit 1
            else
              floating_address = addresses[free_floating].ip
            end
          end
          server.associate_address(floating_address)
          #a bit of a hack, but server.reload takes a long time
          (server.addresses['public'] ||= []) << {"version"=>4,"addr"=>floating_address}
          msg_pair("Floating IP Address", floating_address)
        end

        Chef::Log.debug("Addresses #{server.addresses}")
        Chef::Log.debug("Public IP Address actual: #{primary_public_ip_address(server.addresses)}") if primary_public_ip_address(server.addresses)

        msg_pair("Private IP Address", primary_private_ip_address(server.addresses)) if primary_private_ip_address(server.addresses)

        #which IP address to bootstrap
        bootstrap_ip_address = primary_public_ip_address(server.addresses) if primary_public_ip_address(server.addresses)
        if config[:private_network]
          bootstrap_ip_address = primary_private_ip_address(server.addresses)
        end

        if server.addresses.nil?
          ui.error("No addresses found for server")
          exit 1
        end

        # get the network entry
        network_entry = server.addresses.first
        if network_entry.nil?
          ui.error("No Networks available for bootstrapping.")
          exit 1
        end

        if network_entry.last.last.include? 'addr'
          bootstrap_ip_address = network_entry.last.last['addr'] if bootstrap_ip_address.nil?
        end

        Chef::Log.debug("Bootstrap IP Address #{bootstrap_ip_address}")
        if bootstrap_ip_address.nil?
          ui.error("No IP address available for bootstrapping.")
          exit 1
        end

        puts ui.color("Bootstrap IP Address #{bootstrap_ip_address}", :cyan)

        Chef::Log.debug("Waiting for sshd on IP address: #{bootstrap_ip_address} and port: #{locate_config_value(:ssh_port)}")
        print "\n#{ui.color("Waiting for sshd", :magenta)}"
        print(".") until tcp_test_ssh(bootstrap_ip_address, locate_config_value(:ssh_port)) {
          sleep @initial_sleep_delay ||= 10
          puts("done")
        }

        return { "status" => 0, "ipaddress" => bootstrap_ip_address }

      end
    end
  end
end

