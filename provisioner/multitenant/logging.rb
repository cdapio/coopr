# shared logging module

require 'logger'

module Loom
module Logging
  attr_accessor :level
  @out = nil
  def log
    #puts "log called"
    Loom::Logging.log
  end

  def self.configure(out)
    #puts "self.configure called"
    if out != 'STDOUT'
      @out = out
    end
  end

  def self.level=(level)
    #puts "self.level called, setting level to #{level}"
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
    #puts "level set to #{@level}"
  end

  def self.log
    #puts "self.log called"
    unless @logger
      if @out
        #puts "**** creating new logger to #{@out}"
        @logger = ::Logger.new(@out, 'daily')
      else
        #puts "**** creating new logger to STDOUT"
        @logger = ::Logger.new(STDOUT)
      end
      @logger.level = @level
      @logger.formatter = proc do |severity, datetime, progname, msg|
        "#{datetime} #{severity}: #{msg}\n"
      end
      #puts @logger.inspect
    end
    @logger
  end
end
end
