#!/usr/bin/env ruby

require 'logger'

require_relative 'utils'

module Loom
  class Worker

    attr_accessor :name, :log

    def initialize(name)
      @name = name
      @log = Logger.new("log/worker-#{name}.log")
      @log.level = ::Logger::INFO
      @log.formatter = proc do |severity, datetime, progname, msg|
        "#{datetime} #{severity}: #{msg}\n"
      end
    end

    def work
      log.info("worker #{@name} starting up")
      loop {
        log.info("#{@name} iteration")

        # While provisioning, don't allow the provisioner to terminate by disabling signal
        sigterm = Loom::SignalHandler.new('TERM')
        sigterm.dont_interrupt {

          sleep 30 
          # random chance of dying
          num = Random.rand(20)
          log.info("random number #{num}")
          if num == 0
            log.info("I am going to randomly die")
            raise "I randomly died"
          end
        }

      }
    end 
  end
end

if __FILE__ == $0
  name = ARGV[0] || "default"
  worker = Loom::Worker.new("provisioner-#{name}")
  worker.work
end

