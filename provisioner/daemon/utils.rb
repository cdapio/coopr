#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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

require 'net/scp'

class SignalHandler
  def initialize(signal)
    @interuptable = false
    @enqueued     = []
    trap(signal) do
      if @interuptable
        log.info 'Gracefully shutting down provisioner...'
        exit 0
      else
        @enqueued.push(signal)
      end
    end
  end

  # If this is called with a block then the block will be run with
  # the signal temporarily ignored. Without the block, we'll just set
  # the flag and the caller can call `allow_interuptions` themselves.
  def dont_interupt
    @interuptable = false
    @enqueued     = []
    if block_given?
      yield
      allow_interuptions
    end
  end

  def allow_interuptions
    @interuptable = true
    # Send the temporarily ignored signals to ourself
    # see http://www.ruby-doc.org/core/classes/Process.html#M001286
    @enqueued.each { |signal| Process.kill(signal, 0) }
  end
end

# Exception class used to return remote command stderr
class CommandExecutionError < RuntimeError
  attr_reader :stdout, :stderr, :exit_code, :exit_signal

  def initialize(stdout, stderr, exit_code, exit_signal)
    @stdout = stdout
    @stderr = stderr
    @exit_code = exit_code
    @exit_signal = exit_signal
  end

end

def ssh_exec!(ssh, command)
  stdout_data = ''
  stderr_data = ''
  exit_code = nil
  exit_signal = nil
  log.debug "---ssh-exec command: #{command}"
  ssh.open_channel do |channel|
    channel.exec(command) do |ch, success|
      unless success
        abort "FAILED: couldn't execute command (ssh.channel.exec)"
      end
      channel.on_data do |ch, data|
        stdout_data += data
      end

      channel.on_extended_data do |ch, type, data|
        stderr_data += data
      end

      channel.on_request('exit-status') do |ch, data|
        exit_code = data.read_long
      end

      channel.on_request('exit-signal') do |ch, data|
        exit_signal = data.read_long
      end
    end
  end
  ssh.loop

  log.debug "stderr: #{stderr_data}"
  log.debug "stdout: #{stdout_data}"

  fail CommandExecutionError.new(stdout_data, stderr_data, exit_code, exit_signal), 'Command execution failed' unless exit_code == 0

  [stdout_data, stderr_data, exit_code, exit_signal]
end

# shared logging module
module Logging
  attr_accessor :level
  @out = nil
  def log
    Logging.log
  end

  def configure(out)
    if out != 'STDOUT'
      @out = out
    end
  end

  def level=(level)
    case level
    when /debug/i
      @level = Logger::DEBUG
    when /info/i
      @level = Logger::INFO
    when /warn/i
      @level = Logger::WARN
    when /error/i
      @level = Logger::ERROR
    when /fatal/i
      @level = Logger::FATAL
    else
      @level = Logger::INFO
    end
  end

  def self.log
    unless @logger
      if @out
        @logger = Logger.new(@out, 'daily')
      else
        @logger = Logger.new(STDOUT)
      end
      @logger.level = @level
      @logger.formatter = proc do |severity, datetime, progname, msg|
        "#{datetime} #{severity}: #{msg}\n"
      end
    end
    @logger
  end
end
