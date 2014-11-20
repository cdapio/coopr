require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'aufs::package' do
  include Helpers::Aufs

  # case node['platform']
  # when 'fedora'
  #   p = 'docker-io'
  # when 'ubuntu'
  #   p = 'lxc-docker'
  # end

  # it 'installs docker package' do
  #   package(p).must_be_installed
  # end
end
