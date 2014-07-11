#!/usr/bin/env ruby

require_relative 'tenantspec'
require_relative 'worker'
require_relative 'logging'

module Loom
  class TenantManager
    include Logging
    attr_accessor :spec, :provisioner_id

    def initialize(spec)
#      @logger = Logger.new('/Users/derek/git/loom/provisioner/multitenant/log-tmgr.log')
#      @logger.info "tenanmgr starting"
      if !spec.instance_of?(TenantSpec)
        raise ArgumentError, "TenantManager needs to be initialized with object of type TenantSpec", caller
      end
      @spec = spec
      @provisioner_id = 'default'
      @workerpids = []
      @terminating_workers = []
    end 

    def id
      @spec.id
    end

    def num_workers
      @workerpids.size
    end

    def spawn
      @spec.workers.times do |i|
        spawn_worker_process
      end
    end

    # this is called from the provisioner CLD signal handler upon child process termination
    def verify_workers
      #puts "checking: #{@workerpids}"
      workerpids = @workerpids.dup
      workerpids.each do |pid|
        begin
          log.debug "checking: #{pid}"
          ret = Process.waitpid(pid, Process::WNOHANG)
          if ret == pid
            # child has died
            log.debug "confirmed pid #{pid} dead"
            @workerpids.delete_if {|x| x == pid }
            @terminating_workers.delete(pid) if @terminating_workers.include?(pid)
            log.debug "new workerpids: #{@workerpids}"
          elsif ret.nil?
            #puts "child #{pid} still running"
            # all good, child is running`
          else
            raise "dont know how this can happen"
          end
        rescue Errno::ECHILD
          # pid exists but is not my child
          log.debug "non-child pid: #{pid}"
          @workerpids.delete_if {|x| x == pid }
          @terminating_workers.delete(pid) if @terminating_workers.include?(pid)
          log.debug "new workerpids: #{@workerpids}"
        end
      end
    end

    def spawn_worker_process
      worker_name = "worker-" + self.id + "-"  + (@workerpids.size + 1).to_s
      log.debug "spawning #{worker_name}"
#      @logger.info "spawning #{worker_name}"
#      @logger.info "#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} worker.rb #{worker_name}"
      cpid = fork { 
        #worker = Loom::Worker.new(worker_name)
        #worker.work

        # Absolute paths needed for daemon mode!
        #exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} /Users/derek/git/loom/provisioner/multitenant/worker.rb #{worker_name}")
        exec("#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])} ../daemon/provisioner.rb --tenant #{@spec.id} --provisioner #{@provisioner_id} --uri http://localhost:55054 -l /tmp/worker-#{worker_name}.log -L debug")
      }

      #@logger.info "spawned #{cpid}"
      @workerpids.push(cpid)
    end

    def terminate_worker_process(pid)
      Process.kill(:SIGTERM, pid)
    end

    def terminate_all_worker_processes
      # send SIGTERM to process *group*.  this includes parent
      Process.kill(-15, Process.getpgrp)
    end 


    def update(new_tm)
      log.info "update workers from #{@spec.workers} to #{new_tm.spec.workers}"
      difference = new_tm.spec.workers - @spec.workers
      if difference > 0
        log.debug "adding #{difference} workers"
        difference.times do |i|
          spawn_worker_process
        end
      elsif difference < 0
        log.debug "terminating #{difference.abs} workers"
        # we need to find active workers
        pids_to_kill = []
        @workerpids.reverse_each do |pid|
          break if pids_to_kill.size == difference.abs
          pids_to_kill.push(pid) unless @terminating_workers.include?(pid)
        end

        if pids_to_kill.size != difference.abs
          fail "attempting to kill #{difference.abs} workers but could not find enough running workers to kill"
        end

        pids_to_kill.each do |pid|
          #terminate_worker
          @terminating_workers.push(pid)
          terminate_worker_process(pid)
        end
      end
      @spec.workers = new_tm.spec.workers
    end

    def halt
    end

    def delete
      workerpids = @workerpids.dup 
      workerpids.each do |pid|
        terminate_worker_process(pid)
      end
    end

  end
end


if __FILE__ == $0
  ts = Loom::TenantSpec.new('test', 3)
  tm = Loom::TenantManager.new(ts)
  tm.spawn
  sleep 5
  loop {
    puts "workers running: #{tm.num_workers}"
    sleep 5
  }

end
