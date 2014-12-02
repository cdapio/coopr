require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'aufs::module' do
  include Helpers::Aufs

  it 'enables aufs module' do
    assert_sh('modprobe -n -v aufs')
  end
end
