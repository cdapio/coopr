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

module FogProvider

  # used by ssh validation in confirm stage
  def set_credentials(sshauth)
    @credentials = Hash.new
    @credentials[:paranoid] = false
    sshauth.each do |k, v|
      if (k =~ /identityfile/)
        @credentials[:keys] = [ v ]
      elsif (k =~ /password/)
        @credentials[:password] = v
      end
    end
  end

  # Open a connection to a port
  def tcp_test_port(host, port)
    tcp_socket = TCPSocket.new(host, port)
    readable = IO.select([tcp_socket], nil, nil, 5)
    if readable
      log.debug("Accepting connections on #{host} port #{port}, banner: #{tcp_socket.gets}")
      true
    else
      false
    end
  rescue SocketError,
         IOError,
         Errno::ETIMEDOUT,
         Errno::EPERM,
         Errno::ECONNREFUSED,
         Errno::ECONNRESET,
         Errno::EHOSTUNREACH,
         Errno::ENETUNREACH
    sleep 2
    false
  ensure
    tcp_socket && tcp_socket.close
  end

  def wait_for_sshd(host, port = 22)
    ssh_test_max = 10*60
    ssh_test = 0
    log.debug 'Waiting for sshd'
    # Initial sleep, to prevent getting caught by provider firewall rules
    sleep 30
    begin
      until tcp_test_port(host, port)
        if ssh_test < ssh_test_max
          ssh_test += 1
        else
          break
        end
      end
      sleep 10
    end
  end

  def is_linklocal(ip)
    linklocal = IPAddr.new '169.254.0.0/16'
    return linklocal.include?(ip)
  end

  def is_loopback(ip)
    loopback = IPAddr.new '127.0.0.0/8'
    return loopback.include?(ip)
  end

  def is_private(ip)
    block_a = IPAddr.new '10.0.0.0/8'
    block_b = IPAddr.new '172.16.0.0/12'
    block_c = IPAddr.new '192.168.0.0/16'
    return (block_a.include?(ip) || block_b.include?(ip) || block_c.include?(ip))
  end

end

