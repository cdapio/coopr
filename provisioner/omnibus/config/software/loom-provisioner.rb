name "loom-provisioner"
version "0.9.3"

dependency "ruby"
dependency "rubygems"
dependency "bundler"
dependency "zlib"
dependency "libxml2"
dependency "libxslt"
dependency "nokogiri"

# source :git => "git://github.com/continuuity/loom.git"
# relative_path 'provisioner'

build do
  gem "install knife-joyent --no-rdoc --no-ri --version 0.3.2"
  gem "install knife-rackspace --no-rdoc --no-ri --version 0.8.4"
  gem "install knife-openstack --no-rdoc --no-ri --version 0.8.1"
  command "cp -pPR /var/tmp/loom/provisioner/* ."
  command "mkdir -p bin"
  command "cp /var/tmp/loom/bin/loom-provisioner.sh bin"
  command "sed -e 's/APP_NAME/loom-provisioner/g' -e 's/SVC_NAME/provisioner/g' /var/tmp/loom/bin/loom-service > bin/init-loom-provisioner"
  command "chmod a+x bin/*"
  command "mkdir -p #{install_dir}/etc/logrotate.d"
  command "cp -pP /var/tmp/loom/distribution/etc/logrotate.d/loom-provisioner #{install_dir}/etc/logrotate.d"
  command "cp -pPR bin daemon #{install_dir}"
  gem "uninstall fog -v1.20.0"
  gem "uninstall nokogiri -v1.6.1"
  gem "uninstall -Ix rdoc"
end
