#!/usr/bin/env ruby

require_relative 'tenantmanager'

module Loom
  class Provisioner
    attr_accessor :tenantmanagers

    def initialize()
      @tenantmanagers = {}

      # register signal handler here, and set callback to notify all tenantmanangers
      Signal.trap('CLD') do
        puts "********** Provisioner callback Received CLD signal ***********"
        @tenantmanagers.each do |k, v|
          v.verify_children
        end
      end

      Signal.trap('TERM') do
        if !@shutting_down
          @shutting_down = true
          @tenantmanagers.each do |k, v|
            v.terminate_all_worker_processes
          end
          Process.waitall
          puts "provisioner shutdown complete"
          exit
        end
      end

    end 

    def add_tenant(tenantmgr)
      if !tenantmgr.instance_of?(TenantManager)
        raise ArgumentError, "only instances of TenantManager can be added to provisioner", caller
      end
      # validate input
      id = tenantmgr.id
      puts "Adding/Editing tenant: #{id}"
      raise "cannot add a TenantManager without an id: #{tenantmgr.inspect}" if id.nil?

      if @tenantmanagers.key? id
        # edit tenant
        puts "Editing tenant: #{id}"
        @tenantmanagers[id].update(tenantmgr)
      else
        # new tenant
        puts "Adding new tenant: #{id}"
        tenantmgr.spawn
        @tenantmanagers[id] = tenantmgr
      end 
 
    end

    def delete_tenant(id)
      @tenantmanagers[id].delete
      @tenantmanagers.delete(id)
    end

    def status
      @tenantmanagers.each do |id, tm|
        tm.check_threads
      end
    end

    def heartbeat
      hb = {}
      hb['total'] = 1000
      hb['used'] = {}
      @tenantmanagers.each do |id, tm|
        hb['used'][id] = tm.num_workers
      end
      hb
    end

  end
end
