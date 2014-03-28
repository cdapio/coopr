name             'hadoop_wrapper'
maintainer       'Continuuity'
maintainer_email 'ops@continuuity.com'
license          'All rights reserved'
description      'Hadoop wrapper'
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version '0.1.1'

%w[ apt java hadoop yum krb5 krb5_utils ].each do |cb|
  depends cb
end
