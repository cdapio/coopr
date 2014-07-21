# encoding: UTF-8
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
        opts.on('-l', '--log-file FILE', 'Path to logfile') do |f|
          options[:log_file] = f
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
