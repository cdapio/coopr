#!/usr/bin/env ruby

require 'tempfile'

require_relative 'utils'
require_relative 'logging'

module Loom
  class Worker
    include Logging

      Logging.configure('/Users/derek/git/loom/provisioner/multitenant/workerlog.txt')
      Logging.level = 0

    attr_accessor :name

    def initialize(name)
      @name = name
#      Logging.configure('/Users/derek/git/loom/provisioner/multitenant/workerlog.txt')
#      Logging.level = 0
#      @logger = Logger.new('/Users/derek/git/loom/provisioner/multitenant/log-worker.log')
#      @logger.info "starting"
      log.info "* worker #{name} starting"
#      @log = Logger.new("log/worker-#{name}.log")
#      @log.level = ::Logger::INFO
#      @log.formatter = proc do |severity, datetime, progname, msg|
#        "#{datetime} #{severity}: #{msg}\n"
#      end
      @sigterm = Loom::SignalHandler.new('TERM')
    end

    def work
      #Process.daemon(true)
      log.info("worker #{@name} starting up")
      #sigterm = Loom::SignalHandler.new('TERM')
      loop {
#        log.info("#{@name} iteration")

        sleeptime = Random.rand(20)
        # printing to stdout appears to cause contention 
        puts "worker #{@name} sleeping #{sleeptime}"
        log.info "worker #{@name} sleeping #{sleeptime}"
        # While provisioning, don't allow the provisioner to terminate by disabling signal
        @sigterm.dont_interrupt {
          sleep sleeptime
          # random chance of dying
          num = Random.rand(100)
#          @logger.info "worker #{@name} slept"
#          log.info("random number #{num}")
#          if num == 0
#            log.info("I am going to randomly die")
            #raise "I randomly died"
#          end
        }
        # this appears to be needed to allow time for the signal handler to pass the signal to the OS before 
        # going into the next iteration of the loop
        #sleep 2

        # meaningless interruptable calculation
        100.times do
          foo = "bar"
        end

      }
    end 
  end
end

if __FILE__ == $0
  name = ARGV[0] || "default"
#  logger2 = Logger.new('/Users/derek/git/loom/provisioner/multitenant/log-workerinit.log')
#  logger2.info "creating worker obj"
  worker = Loom::Worker.new("provisioner-#{name}")
#  logger2.info "working"
  worker.work
end

