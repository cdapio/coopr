#!/usr/bin/env ruby
# encoding: UTF-8

module Loom
  class TenantSpec
    attr_accessor :id, :workers, :modules, :plugins

    def initialize(id, workers, modules = nil, plugins = nil)
      @id = id
      @workers = workers
      @modules = modules ||= Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
      @plugins = plugins ||= Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
    end
  end
end
