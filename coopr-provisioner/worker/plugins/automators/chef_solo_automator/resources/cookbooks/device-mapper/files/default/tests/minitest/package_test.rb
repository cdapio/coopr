require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'device-mapper::package' do
  include Helpers::DeviceMapper

  describe 'on centos/fedora/oracle/redhat' do
    before do
      skip('centos/fedora/oracle/redhat only') unless %w(centos fedora oracle redhat).include?(node['platform'])
    end

    packages = %w(device-mapper device-mapper-devel device-mapper-persistent-data)

    it 'installs device-mapper packages' do
      packages.each do |p|
        package(p).must_be_installed
      end
    end
  end

  describe 'on debian/ubuntu' do
    before do
      skip('debian/ubuntu only') unless %w(debian ubuntu).include?(node['platform'])
    end

    packages = %w(libdevmapper1.02.1 libdevmapper-dev)

    it 'installs device-mapper packages' do
      packages.each do |p|
        package(p).must_be_installed
      end
    end
  end

  it 'installs device-mapper module' do
    assert_sh('modprobe -n -v dm-mod')
  end
end
