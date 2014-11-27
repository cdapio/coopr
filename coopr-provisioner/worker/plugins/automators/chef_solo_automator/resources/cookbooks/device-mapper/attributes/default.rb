default['device-mapper']['packages'] = value_for_platform(
  %w(centos fedora oracle redhat) => {
    'default' => %w(device-mapper device-mapper-devel device-mapper-persistent-data)
  },
  %w(debian ubuntu) => {
    'default' => %w(libdevmapper1.02.1 libdevmapper-dev)
  }
)
