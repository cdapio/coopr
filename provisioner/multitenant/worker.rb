#!/usr/bin/env ruby

require 'tempfile'

require_relative 'utils'
require_relative 'logging'

module Loom
  class Worker
    include Logging

    attr_accessor :name

    def initialize(name)
      @name = name
      Logging.configure("#{File.dirname(__FILE__)}/#{@name}.log")
      Logging.level = 0
      log.info "* worker #{name} starting"
      @sigterm = Loom::SignalHandler.new('TERM')
    end

    def work
      log.info("worker #{@name} starting up")
      loop {
        sleeptime = Random.rand(20)
        # printing to stdout appears to cause contention 
        puts "worker #{@name} sleeping #{sleeptime}"
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
  name = ARGV[0] || "worker-default"
  worker = Loom::Worker.new(name)
  worker.work
end

