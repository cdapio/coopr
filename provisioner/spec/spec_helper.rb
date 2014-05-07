require 'simplecov'
SimpleCov.start do
  add_filter '/spec/'
end

require_relative '../daemon/automator'
require_relative '../daemon/pluginmanager'
require_relative '../daemon/provider'
require_relative '../daemon/utils'
