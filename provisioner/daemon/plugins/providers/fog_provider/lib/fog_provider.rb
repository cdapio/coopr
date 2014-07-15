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

gem 'fog', '~> 1.21.0'

require 'fog'

class FogProvider < Provider

  # used by ssh validation in confirm stage
  def set_credentials(sshauth)
    @credentials = Hash.new
    @credentials[:paranoid] = false
    sshauth.each do |k, v|
      if (k =~ /password/)
	@credentials[:password] = v
      elsif (k =~ /identityfile/)
	@credentials[:keys] = [ v ]
      end
    end
  end

  # Open a connection to a port
  def tcp_test_port(hostname, port)
    tcp_socket = TCPSocket.new(hostname, port)
    readable = IO.select([tcp_socket], nil, nil, 5)
    if readable
      log.debug("sshd accepting connections on #{hostname} port #{port}, banner is #{tcp_socket.gets}")
      yield
      true
    else
      false
    end
  rescue Errno::ETIMEDOUT
    false
  rescue Errno::EPERM
    false
  rescue Errno::ECONNREFUSED
    sleep 2
    false
  rescue Errno::EHOSTUNREACH, Errno::ENETUNREACH
    sleep 2
    false
  rescue Errno::ENETUNREACH
    sleep 2
    false
  ensure
    tcp_socket && tcp_socket.close
  end

  def wait_for_sshd
    log.debug 'Waiting for sshd'
    until tcp_test_port(bootstrap_ip, 22) {
      sleep @initial_sleep_delay ||= 10
      log.info "Server #{server.name} sshd is up"
    }
    end
  end

end

