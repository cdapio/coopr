name             'hadoop_wrapper'
maintainer       'Cask Data, Inc'
maintainer_email 'ops@cask.co'
license          'All rights reserved'
description      'Hadoop wrapper'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.12'

%w(apt java krb5_utils yum).each do |cb|
  depends cb
end

depends 'hadoop', '>= 1.3.0'
depends 'mysql', '< 5.0.0'
depends 'database', '< 2.1.0'
depends 'krb5', '>= 1.0.0'
