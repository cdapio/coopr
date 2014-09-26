#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
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


require 'tempfile'

require_relative 'utils'
require_relative '../lib/provisioner/logging'

module Coopr
  class Worker
    include Logging

    attr_accessor :name

    def initialize(tenant)
      pid = Process.pid
      @name = "worker.#{tenant}.#{pid}"
      #Logging.configure("#{File.dirname(__FILE__)}/#{@name}.log")
      Logging.level = 0
      log.info "* worker #{name} starting"
      @sigterm = Coopr::SignalHandler.new('TERM')
    end

    def work
      log.info("worker #{@name} starting up")
      loop {
        sleeptime = Random.rand(20)
        #puts "worker #{@name} sleeping #{sleeptime}"
        log.info "worker #{@name} sleeping #{sleeptime}"
        # While provisioning, don't allow the provisioner to terminate by disabling signal
        @sigterm.dont_interrupt {
          sleep sleeptime
        }
      }
    end 
  end
end

if __FILE__ == $0
  tenant = ARGV[0] || "superadmin"
  worker = Coopr::Worker.new(tenant)
  worker.work
end

