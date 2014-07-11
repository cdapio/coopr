#!/usr/bin/env ruby

require_relative 'tenantmanager'
require_relative 'logging'

module Loom
  class Provisioner
    include Logging
    attr_accessor :tenantmanagers, :provisioner_id

    #Logging.configure(options[:log_file])
    #Logging.level = options[:log_level]
    #configure(options[:log_file])
    #level = options[:log_level]


    def initialize()
      @tenantmanagers = {}
      @terminating_tenants = []
      pid = Process.pid
      host = Socket.gethostname.downcase
      @provisioner_id = "#{host}.#{pid}"
      log.info "provisioner #{@provisioner_id} initialized"
    end 

    def add_tenant(tenantmgr)
      if !tenantmgr.instance_of?(TenantManager)
        raise ArgumentError, "only instances of TenantManager can be added to provisioner", caller
      end
      # validate input
      id = tenantmgr.id
      log.info "Adding/Editing tenant: #{id}"
      raise "cannot add a TenantManager without an id: #{tenantmgr.inspect}" if id.nil?

      # set provisionerId
      tenantmgr.provisioner_id = @provisioner_id

      if @tenantmanagers.key? id
        # edit tenant
        log.info "Editing tenant: #{id}"
        @tenantmanagers[id].update(tenantmgr)
      else
        # new tenant
        log.info "Adding new tenant: #{id}"
        tenantmgr.spawn
        @tenantmanagers[id] = tenantmgr
      end 
    end

    def verify_tenants
      @tenantmanagers.each do |k, v|
        v.verify_workers
        # has this tenant been deleted?
        if (@terminating_tenants.include?(k) && v.num_workers == 0)
          @tenantmanagers.delete(k)
          @terminating_tenants.delete(k) 
        end
      end
    end

    def delete_tenant(id)
      # instruct tenant to send kill signal to its workers
      @tenantmanagers[id].delete
      # we mark this tenant as deleting.  when its num_workers reaches 0 it can be deleted from @tenantmanagers
      @terminating_tenants.push(id)
    end

    def status
      @tenantmanagers.each do |id, tm|
        tm.check_threads
      end
    end

    def heartbeat
      hb = {}
      hb['usage'] = {}
      @tenantmanagers.each do |id, tm|
        hb['usage'][id] = tm.num_workers
      end
      hb
    end

  end
end
