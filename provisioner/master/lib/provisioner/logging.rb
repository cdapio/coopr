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

# shared logging module

require 'logger'

module Loom
  module Logging
    attr_accessor :level
    @level = ::Logger::INFO
    @shift_age = nil
    @shift_size = nil
    @process_name = '-'
    @out = nil
    def log
      Loom::Logging.log
    end

    def self.configure(out)
      if out != 'STDOUT'
        @out = out
      end
    end

    def self.level=(level)
      case level
      when /debug/i
        @level = ::Logger::DEBUG
      when /info/i
        @level = ::Logger::INFO
      when /warn/i
        @level = ::Logger::WARN
      when /error/i
        @level = ::Logger::ERROR
      when /fatal/i
        @level = ::Logger::FATAL
      else
        @level = ::Logger::INFO
      end
    end

    def self.shift_age=(shift_age)
      @shift_age = shift_age
    end

    def self.shift_size=(shift_size)
      @shift_size = shift_size
    end

    def self.process_name=(process_name)
      @process_name = process_name
    end

    def self.log
      unless @logger
        if @out
          @logger = ::Logger.new(@out, @shift_age.to_i, @shift_size.to_i)
        else
          @logger = ::Logger.new(STDOUT)
        end
        @logger.level = @level
        @logger.formatter = proc do |severity, datetime, progname, msg|
          "#{datetime} #{@process_name} #{severity}: #{msg}\n"
        end
      end
      @logger
    end
  end
end
