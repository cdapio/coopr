#!/usr/bin/env ruby

require_relative 'tenantspec'
require_relative 'worker'

module Loom
  class TenantManager
    attr_accessor :spec, :provisioner_id
    @workerthreads = []
    @workerpids = []

    def initialize(spec)
      if !spec.instance_of?(TenantSpec)
        raise ArgumentError, "TenantManager needs to be initialized with object of type TenantSpec", caller
      end
      @spec = spec
      @workerthreads = []
      @workerpids = []
    end 

    def id
      @spec.id
    end

    def num_workers
      @workerpids.size
    end

    # this is called from the provisioner CLD signal handler upon child process termination
    def pid_killed(pid)
      puts "workerpids before delete: #{@workerpids}"
      @workerpids.delete_if {|x| x == pid }
      puts "workerpids after delete: #{@workerpids}"
    end

    def spawn
      @spec.workers.times do |i|
        #spawn_worker_thread
        spawn_worker_process
      end
    end

    def verify_children
      @workerpids.each do |pid|
        begin
          ret = Process.waitpid(pid, Process::WNOHANG)
          if ret == pid
            # child has died
            #puts "confirmed pid #{pid} dead"
            @workerpids.delete_if {|x| x == pid }
          elsif ret.nil?
            # all good, child is running`
          else
            raise "dont know how this can happen"
          end
        rescue Errno::ECHILD
          # pid exists but is not my child
          puts "non-child pid: #{pid}"
        end
      end
    end

#    def spawn_worker_thread
#      worker_name = "worker-" + self.id + "-"  + (@workerthreads.size + 1).to_s
#      puts "spawning #{worker_name}"
#      thr = Thread.new do
#        worker = Loom::Worker.new(worker_name)
#        worker.work
#      end
#      @workerthreads.push(thr)
#    end

    def spawn_worker_process
      worker_name = "worker-" + self.id + "-"  + (@workerpids.size + 1).to_s
      puts "spawning #{worker_name}"
      cpid = fork { 
        #worker = Loom::Worker.new(worker_name)
        #worker.work
        #exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} worker.rb #{worker_name}")
        exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} ../daemon/provisioner.rb --tenant #{@spec.id} --provisioner #{@provisioner_id} --uri http://localhost:55055")
      }

      @workerpids.push(cpid)
    end

    def terminate_worker_process(pid)
      Process.kill(:SIGTERM, pid)
#      Process.waitpid(pid_to_kill)
#      while true do
#        if @workerpids.include? pid_to_kill
#          puts "waiting for confirmation pid #{pid_to_kill} is dead"
#          puts "workerpids: #{@workerpids}"
#          sleep 1
#        else
#          break
#        end
#      end
    end

    def terminate_all_worker_processes
      # send SIGTERM to process *group*.  this includes parent
      Process.kill(-15, Process.getpgrp)
#      copyworkerpids.each do |pid|
#        puts "sending kill to pid #{pid}"
#        terminate_worker_process(pid)
#        sleep 1
#      end
    end 

#    def terminate_worker_thread
#      @workerthreads.pop.terminate
#    end

    def update(new_tm)
      puts "update workers to #{new_tm.spec.workers}"
      @spec.workers = new_tm.spec.workers
      difference = @spec.workers - @workerpids.size
      if difference > 0
        puts "adding #{difference} workers"
        difference.times do |i|
          #spawn_worker_thread
          spawn_worker_process
        end
      elsif difference < 0
        puts "terminating #{difference.abs} workers"
        pids_to_kill = @workerpids[difference, difference.abs]
        puts "pids_to_kill: #{pids_to_kill}"
        pids_to_kill.each do |pid|
          #terminate_worker
          terminate_worker_process(pid)
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
