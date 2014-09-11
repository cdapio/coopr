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

require 'optparse'

module Loom
  class CLI
    def self.read(arguments = ARGV)

      # Parse command line options.
      options = {}
      OptionParser.new do |opts|
        opts.banner = 'Usage: '
        opts.on('-c', '--config-file FILE', 'Site-specific config file to use') do |c|
          options[:configfile] = c
        end
        opts.on('-r', '--register', 'Register installed plugins with the server.') do
          options[:register] = true
        end
      end.parse!(arguments)
      options
    end
  end
end
