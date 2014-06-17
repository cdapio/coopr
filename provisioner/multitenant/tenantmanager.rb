#!/usr/bin/env ruby

require_relative 'tenantspec'
require_relative 'worker'

module Loom
  class TenantManager
    attr_accessor :spec
    @workerthreads = []

    def initialize(spec)
      if !spec.instance_of?(TenantSpec)
        raise ArgumentError, "TenantManager needs to be initialized with object of type TenantSpec", caller
      end
      @spec = spec
      @workerthreads = []
    end 

    def id
      @spec.id
    end

    def num_workers
      @workerthreads.size
    end

    def spawn
      @spec.workers.times do |i|
        spawn_worker
      end
    end

    def spawn_worker
      worker_name = "worker-" + self.id + "-"  + (@workerthreads.size + 1).to_s
      puts "spawning #{worker_name}"
      thr = Thread.new do
        worker = Loom::Worker.new(worker_name)
        worker.work
      end
      @workerthreads.push(thr)
    end

    def terminate_worker
      @workerthreads.pop.terminate
    end

    def update(new_tm)
      puts "update to #{new_tm.inspect}"

      difference = new_tm.spec.workers - @workerthreads.size
      if difference > 0
        puts "adding #{difference} workers"
        difference.times do |i|
          spawn_worker
        end
      elsif difference < 0
        puts "terminating #{difference.abs} workers"
        difference.abs.times do |i|
          terminate_worker
        end
      end
    end

    def halt
    end

    def delete
      puts "deleting, killing threads"
      @workerthreads.each do |t|
        t.terminate
      end
    end

    def check_threads
      puts "worker #{@spec.id}, known threads: #{@workerthreads.size}"
      @workerthreads.each do |t|
        print "    #{t.inspect}"
        if t.alive?
          print " is ALIVE\n"
        else
          print " is DEAD\n"
        end
      end
    end

  end
end


if __FILE__ == $0
  ts = Loom::TenantSpec.new(3)
  tm = Loom::TenantManager.new(ts)
  tm.spawn
  sleep 5
  loop {
    tm.check_threads
    sleep 5
  }

end
