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

require 'optparse'

module Loom
  class CLI
    def self.read(arguments = ARGV)

      # Parse command line options.
      options = {}
      OptionParser.new do |opts|
        opts.banner = 'Usage: '
        opts.on('-u', '--uri URI', 'Loom web server uri') do |u|
          options[:uri] = u
        end
        opts.on('-f', '--file FILE', 'Full path to task json') do |f|
          options[:file] = f
        end
        opts.on('-t', '--tenant TENANT', 'Tenant ID') do |t|
          options[:tenant] = t
        end
        opts.on('-p', '--provisioner PROVISIONER', 'Provisioner ID') do |p|
          options[:provisioner] = p
        end
        opts.on('-L', '--log-level LEVEL', 'Log level') do |f|
          options[:log_level] = f
        end
        opts.on('-l', '--log-directory DIR', 'Path to log directory for provisioner and workers') do |d|
          options[:log_directory] = d
        end
        opts.on('-b', '--background', 'runs as a daemon. ensure you specify a logfile also') do 
          options[:daemonize] = true 
        end
        opts.on('-r', '--register', 'Register installed plugins with the server.  Requires --uri') do
          options[:register] = true
        end
      end.parse!(arguments)

      if !options[:uri] && !options[:file]
        puts 'Either URI for loom server or --file must be specified'
        exit(1)
      end
      if(options[:uri].nil? && options[:register])
        puts "--register option requires the --uri [server uri] option"
        exit(1)
      end
      options
    end
  end
end
