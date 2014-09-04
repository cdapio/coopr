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


# simple class to construct the ruby command used to launch a worker process
module Loom
  class WorkerLauncher
    attr_accessor :tenant, :provisioner, :name, :config

    def initialize(config)
      @config = config || {}
    end

    def cmd
      prod_cmd
    end

    def prod_cmd
      cmd = "#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])}"
      cmd += " #{File.dirname(__FILE__)}/../../../worker/provisioner.rb"
      cmd += " --uri #{@config.get(PROVISIONER_SERVER_URI)}" if @config.get(PROVISIONER_SERVER_URI)
      cmd += " --log-dir #{@config.get(PROVISIONER_LOG_DIR)}" if @config.get(PROVISIONER_LOG_DIR)
      cmd += " --log-level #{@config.get(PROVISIONER_LOG_LEVEL)}" if @config.get(PROVISIONER_LOG_LEVEL)
      cmd += " --work-dir #{@config.get(PROVISIONER_WORK_DIR)}" if @config.get(PROVISIONER_WORK_DIR)
      cmd += " --provisioner #{@provisioner}" unless @provisioner.nil?
      cmd += " --tenant #{@tenant}" unless @tenant.nil?
      cmd += " --name #{@name}" unless @name.nil?
      cmd
    end

    def test_cmd
      cmd = "#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])}"
      cmd += " #{File.dirname(__FILE__)}/../../spec/worker.rb"
      cmd += " #{@name}" unless @name.nil?
      cmd
    end
  end
end
