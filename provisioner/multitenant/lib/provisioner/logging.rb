# encoding: UTF-8
# shared logging module

require 'logger'

module Loom
  module Logging
    attr_accessor :level
    @level = ::Logger::INFO
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

    def self.log
      unless @logger
        if @out
          @logger = ::Logger.new(@out, 'daily')
        else
          @logger = ::Logger.new(STDOUT)
        end
        @logger.level = @level
        @logger.formatter = proc do |severity, datetime, progname, msg|
          "#{datetime} #{severity}: #{msg}\n"
        end
      end
      @logger
    end
  end
end
