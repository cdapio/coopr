require 'spec_helper'
require 'logger'

include Logging

describe PluginManager do

  # These run before each test
  before :each do
    @pluginmanager = PluginManager.new
  end

#  describe '#new' do
#    it 'creates an instance of PluginManager' do
#      expect(pluginmanager).to be_an_instance_of PluginManager
#    end
#  end

end
