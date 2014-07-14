#!/usr/bin/env ruby
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

class FogProviderRackspaceConfirm

  def run
    $stdout.sync = true

    id = name_args.first
    log.debug "fetching server for id: #{id}"
    server = self.connection.servers.get(id)

    # Wait until the server is ready
    begin
      server.wait_for(600) { ready? }
    rescue Fog::Errors::TimeoutError
      log.error 'Timeout waiting for the server to be created'
    end

    bootstrap_ip = ip_address(server, 'public')
    if bootstrap_ip.nil?
      log.error 'No IP address available for bootstrapping.'
      exit 1
    else
      log.debug "Bootstrap IP address #{bootstrap_ip}"
    end

    wait_for_sshd

    return { 'status' => 0, 'ipaddress' => bootstrap_ip }

  end
end
