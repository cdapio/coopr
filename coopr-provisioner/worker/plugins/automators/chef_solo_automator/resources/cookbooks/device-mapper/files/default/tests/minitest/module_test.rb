require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'device-mapper::module' do
  include Helpers::DeviceMapper

  it 'enables device-mapper module' do
    assert_sh('modprobe -n -v dm-mod')
  end
end
