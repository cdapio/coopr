#!/usr/bin/env ruby
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


require_relative 'tenantspec'
require_relative 'logging'
require_relative 'workerlauncher'
require_relative 'resourcemanager'

module Coopr 
  class TenantManager
    include Logging
    attr_accessor :spec, :resourcemanager
    attr_reader :status, :config, :provisioner_id, :sync_requests

    def initialize(spec, config, provisioner_id)
      unless spec.instance_of?(TenantSpec)
        raise ArgumentError, "TenantManager needs to be initialized with object of type TenantSpec", caller
      end
      @spec = spec
      @config = config
      @provisioner_id = provisioner_id
      @workerpids = []
      @terminating_workers = []
      @resourcemanager = ResourceManager.new(spec.resourcespec, spec.id, config)
      @sync_requests = 0

    end 

    def id
      @spec.id
    end

    def num_workers
      @workerpids.size
    end

    # launch the specified number of worker processes
    def spawn
      @spec.workers.times do |i|
        spawn_worker_process
      end
    end

    def resource_sync_needed
      @status = 'STALE'
      @sync_requests += 1
      terminate_all_worker_processes
    end

    def resource_sync_needed?
      'STALE' == @status && @sync_requests > 0
    end

    def sync
      @sync_requests = 0
      @resourcemanager.sync
    end

    def resume
      @status = 'ACTIVE'
      spawn
    end


    # check worker processes, called after CLD signal processed (child process termination)
    def verify_workers
      workerpids = @workerpids.dup
      workerpids.each do |pid|
        begin
          log.debug "checking: #{pid}"
          ret = Process.waitpid(pid, Process::WNOHANG)
          if ret == pid
            # child has died
            log.debug "confirmed pid #{pid} dead"
            forget_dead_worker(pid)
          elsif ret.nil?
            # all good, child is running
          else
            # this should never happen
            fail "Process.waitpid returned something other than nil or expected pid: #{ret.inspect}"
          end
        rescue Errno::ECHILD
          # pid exists but is not my child
          log.debug "non-child pid: #{pid}"
          forget_dead_worker(pid)
        end
      end
    end

    # un-register a child pid
    def forget_dead_worker(pid)
      @workerpids.delete_if {|x| x == pid }
      @terminating_workers.delete(pid) if @terminating_workers.include?(pid)
      log.debug "new workerpids: #{@workerpids}"
    end

    # spawn a new child worker process
    def spawn_worker_process
      worker_launcher = WorkerLauncher.new(@config)
      worker_launcher.provisioner = @provisioner_id
      worker_launcher.tenant = id
      worker_launcher.name = "worker-" + id + "-"  + (@workerpids.size + 1).to_s
      worker_cmd = worker_launcher.cmd
      log.debug "spawning #{worker_cmd}"
      cpid = fork { 
        exec(worker_cmd)
      }
      @workerpids.push(cpid)
    end

    def terminate_worker_process(pid)
      @terminating_workers.push(pid)
      Process.kill(:SIGTERM, pid)
    end

    # process new specifications for this tenant
    def update(new_tm)
      # first check for new resource requirements
      unless @spec.resourcespec == new_tm.spec.resourcespec
        resource_sync_needed
        log.debug "current expected resources: #{@spec.resourcespec.resources}"
        @spec = new_tm.spec
        log.debug "new expected resources: #{@spec.resourcespec.resources}"
        @resourcemanager.resourcespec = @spec.resourcespec
        return
      end
      log.debug "update workers from #{@spec.workers} to #{new_tm.spec.workers}"
      difference = new_tm.spec.workers - @spec.workers
      if difference > 0
        log.debug "adding #{difference} workers"
        difference.times do |i|
          spawn_worker_process
        end
      elsif difference < 0
        log.debug "terminating #{difference.abs} workers"
        # we need to find active workers not already terminating
        pids_to_kill = []
        @workerpids.reverse_each do |pid|
          break if pids_to_kill.size == difference.abs
          pids_to_kill.push(pid) unless @terminating_workers.include?(pid)
        end

        if pids_to_kill.size != difference.abs
          fail "attempting to kill #{difference.abs} workers but could not find enough running workers to kill"
        end

        pids_to_kill.each do |pid|
          terminate_worker_process(pid)
        end
      end
      # update our "expected" spec
      @spec.workers = new_tm.spec.workers
    end

    # send kill to all workers
    def terminate_all_worker_processes
      workerpids = @workerpids.dup
      workerpids.each do |pid|
        terminate_worker_process(pid)
      end
    end

    # delete sends kill to all workers
    def delete
      @status = 'DELETING'
      terminate_all_worker_processes
    end

  end
end
