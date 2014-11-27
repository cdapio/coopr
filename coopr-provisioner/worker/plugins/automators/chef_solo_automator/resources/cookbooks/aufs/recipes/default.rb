include_recipe 'apt' if node['platform'] == 'ubuntu'
include_recipe 'aufs::package'
include_recipe 'aufs::module'
