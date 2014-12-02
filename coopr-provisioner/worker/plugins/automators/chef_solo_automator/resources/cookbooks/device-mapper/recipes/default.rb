include_recipe 'apt' if node['platform'] == 'ubuntu'
include_recipe 'device-mapper::package'
include_recipe 'device-mapper::module'
