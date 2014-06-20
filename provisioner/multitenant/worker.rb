#!/usr/bin/env ruby

#require 'logger'

require_relative 'utils'

module Loom
  class Worker

    attr_accessor :name, :log

    def initialize(name)
      @name = name
#      @log = Logger.new("log/worker-#{name}.log")
#      @log.level = ::Logger::INFO
#      @log.formatter = proc do |severity, datetime, progname, msg|
#        "#{datetime} #{severity}: #{msg}\n"
#      end
      @sigterm = Loom::SignalHandler.new('TERM')
    end

    def work
      #Process.daemon(true)
#      log.info("worker #{@name} starting up")
      #sigterm = Loom::SignalHandler.new('TERM')
      loop {
#        log.info("#{@name} iteration")

        sleeptime = Random.rand(20)
        puts "worker #{@name} sleeping #{sleeptime}"
        # While provisioning, don't allow the provisioner to terminate by disabling signal
        @sigterm.dont_interrupt {
          sleep sleeptime
          # random chance of dying
          num = Random.rand(100)
#          log.info("random number #{num}")
#          if num == 0
#            log.info("I am going to randomly die")
            #raise "I randomly died"
#          end
        }
        # this appears to be needed to allow time for the signal handler to pass the signal to the OS before 
        # going into the next iteration of the loop
        #sleep 2

      }
    end 
  end
end

if __FILE__ == $0
  name = ARGV[0] || "default"
  worker = Loom::Worker.new("provisioner-#{name}")
  worker.work
end

