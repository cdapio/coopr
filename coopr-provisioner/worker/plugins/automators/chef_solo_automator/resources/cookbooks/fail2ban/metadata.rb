name              'fail2ban'
maintainer        'Opscode, Inc.'
maintainer_email  'cookbooks@opscode.com'
license           'Apache 2.0'
description       'Installs and configures fail2ban'
version           '2.1.2'

recipe 'fail2ban', 'Installs and configures fail2ban'

depends 'yum', '~> 3.0'
depends 'yum-epel'

%w{ debian ubuntu redhat centos scientific amazon oracle }.each do |os|
  supports os
end
