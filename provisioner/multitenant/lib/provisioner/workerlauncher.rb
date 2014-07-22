#!/usr/bin/env ruby
# encoding: UTF-8

# simple class to construct the ruby command used to launch a worker process
module Loom
  class WorkerLauncher
    attr_accessor :tenant, :provisioner, :name, :options

    def initialize(options)
      @options = options
    end

    def cmd
      cmd = "#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])}"
      cmd += " #{File.dirname(__FILE__)}/../../../daemon/provisioner.rb"
      cmd += " --uri #{@options[:uri]}" if @options[:uri]
      cmd += " --log-directory #{@options[:log_directory]}" if @options[:log_directory]
      cmd += " --log-level #{@options[:log_level]}" if @options[:log_level]
      cmd += " --provisioner #{@provisioner}" unless @provisioner.nil?
      cmd += " --tenant #{@tenant}" unless @tenant.nil?
      cmd += " --name #{@name}" unless @name.nil?
      cmd
    end

    def test_cmd
      cmd = "#{File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['ruby_install_name'])}"
      cmd += " #{File.dirname(__FILE__)}/../../spec/worker.rb"
      cmd += @name unless @name.nil?
      cmd
    end
  end
end