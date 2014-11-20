# Helpers module for minitest
module Helpers
  # Helpers::DeviceMapper module for minitest
  module DeviceMapper
    include MiniTest::Chef::Assertions
    include MiniTest::Chef::Context
    include MiniTest::Chef::Resources
  end
end
