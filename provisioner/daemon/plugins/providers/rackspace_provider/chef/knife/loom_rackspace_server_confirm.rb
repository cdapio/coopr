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
require 'chef/knife/rackspace_server_create'

class Chef
  class Knife
    class LoomRackspaceServerConfirm < RackspaceServerCreate

      # support options provided by rackspace base module
      include Knife::RackspaceBase

      # quick workaround for class level instance vars
      self.options.merge!(RackspaceServerCreate.options)

      banner 'knife loom rackspace server confirm (options) SERVER-ID'

      def run

        unless name_args.size === 1
          show_usage
          exit 1
        end

        
        networks = get_networks(Chef::Config[:knife][:rackspace_networks])
        rackconnect_wait = Chef::Config[:knife][:rackconnect_wait] || config[:rackconnect_wait]
        rackspace_servicelevel_wait = Chef::Config[:knife][:rackspace_servicelevel_wait] || config[:rackspace_servicelevel_wait]

        id = name_args.first
        puts "fetching server for id: #{id}"
        server = self.connection.servers.get(id)

        # start copy from rackspace server create

        # wait for it to be ready to do stuff
        begin
          server.wait_for(1200) { 
            print "."; 
            Chef::Log.debug("#{progress}%")
            if rackconnect_wait and rackspace_servicelevel_wait
              Chef::Log.debug("rackconnect_automation_status: #{metadata.all['rackconnect_automation_status']}")
              Chef::Log.debug("rax_service_level_automation: #{metadata.all['rax_service_level_automation']}")
              ready? and metadata.all['rackconnect_automation_status'] == 'DEPLOYED' and metadata.all['rax_service_level_automation'] == 'Complete'
            elsif rackconnect_wait
              Chef::Log.debug("rackconnect_automation_status: #{metadata.all['rackconnect_automation_status']}")
              ready? and metadata.all['rackconnect_automation_status'] == 'DEPLOYED'
            elsif rackspace_servicelevel_wait
              Chef::Log.debug("rax_service_level_automation: #{metadata.all['rax_service_level_automation']}")
              ready? and metadata.all['rax_service_level_automation'] == 'Complete'
            else
              ready?
            end
          }
        rescue Fog::Errors::TimeoutError
          ui.error('Timeout waiting for the server to be created')
          msg_pair('Progress', "#{server.progress}%")
          msg_pair('rackconnect_automation_status', server.metadata.all['rackconnect_automation_status'])
          msg_pair('rax_service_level_automation', server.metadata.all['rax_service_level_automation'])
          Chef::Application.fatal! 'Server didn\'t finish on time'
        end
        msg_pair("Metadata", server.metadata)
        if(networks && Chef::Config[:knife][:rackspace_networks])
          msg_pair("Networks", Chef::Config[:knife][:rackspace_networks].sort.join(', '))
        end

        print "\n#{ui.color("Waiting server", :magenta)}"

        server.wait_for(Integer(locate_config_value(:server_create_timeout))) { print "."; ready? }
        # wait for it to be ready to do stuff

        puts("\n")

        msg_pair("Public DNS Name", public_dns_name(server))
        msg_pair("Public IP Address", ip_address(server, 'public'))
        msg_pair("Private IP Address", ip_address(server, 'private'))
        msg_pair("Password", server.password)
        msg_pair("Metadata", server.metadata.all)

        bootstrap_ip_address = ip_address(server, config[:bootstrap_network])
        Chef::Log.debug("Bootstrap IP Address #{bootstrap_ip_address}")
        if bootstrap_ip_address.nil?
          ui.error("No IP address available for bootstrapping.")
          exit 1
        end

        print "\n#{ui.color("Waiting for sshd", :magenta)}"
        print(".") until tcp_test_ssh(bootstrap_ip_address) {
          sleep @initial_sleep_delay ||= 10
          puts("done")
        }

        puts ui.color("Bootstrap IP Address #{bootstrap_ip_address}", :cyan)

        return { "status" => 0, "ipaddress" => bootstrap_ip_address }

      end
    end
  end
end

