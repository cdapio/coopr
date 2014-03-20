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
require 'chef/knife/joyent_base'
require 'chef/knife/joyent_server_create'

class Chef
  class Knife
    class LoomJoyentServerConfirm < JoyentServerCreate

      Chef::Knife::JoyentServerCreate.load_deps

      # support options provided by joyent base module
      include Knife::JoyentBase

      # quick workaround for class level instance vars
      self.options.merge!(JoyentServerCreate.options)

      def run

        unless name_args.size === 1
          show_usage
          exit 1
        end

        id = name_args.first

        puts "fetching server for id: #{id}"
        server = self.connection.servers.get(id)

        puts ui.color("Waiting for Server to be Provisioned", :magenta)
        server.wait_for { print "."; ready? }

        bootstrap_ip = self.determine_bootstrap_ip(server)

        unless bootstrap_ip
          puts ui.error("No IP address available for bootstrapping.")
          exit 1
        end

        Chef::Log.debug("Bootstrap IP Address #{bootstrap_ip}")
        puts "\n"
        puts ui.color("Bootstrap IP Address #{bootstrap_ip}", :cyan)

        puts ui.color("Waiting for server to fully initialize...", :cyan)
        sleep 20

        puts ui.color("Waiting for SSH to come up on: #{bootstrap_ip}", :cyan)
        tcp_test_ssh(bootstrap_ip)


        return { "status" => 0, "ipaddress" => bootstrap_ip }

      end
    end
  end
end

