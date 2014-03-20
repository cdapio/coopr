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
    class LoomJoyentServerCreate < JoyentServerCreate

      Chef::Knife::JoyentServerCreate.load_deps

      # support options provided by joyent base module
      include Knife::JoyentBase

      # quick workaround for class level instance vars 
      self.options.merge!(JoyentServerCreate.options)

      banner 'knife loom joyent server create (options)'

      def run
        $stdout.sync = true
        
        validate_server_name
        @node_name = config[:chef_node_name] || config[:server_name]

        puts ui.color("Creating machine #{@node_name}", :cyan)

        server = connection.servers.create(server_creation_options)
        puts "SERVERID: #{server.id.to_s}"
        return { "status" => 0, "providerid" => server.id.to_s }
      end
    end
  end
end

