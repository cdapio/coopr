require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'device-mapper::default' do
  include Helpers::DeviceMapper
end
