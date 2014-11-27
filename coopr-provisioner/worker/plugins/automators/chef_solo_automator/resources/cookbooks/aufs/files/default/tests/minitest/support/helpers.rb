# Helpers module for minitest
module Helpers
  # Helpers::Aufs module for minitest
  module Aufs
    include MiniTest::Chef::Assertions
    include MiniTest::Chef::Context
    include MiniTest::Chef::Resources
  end
end
