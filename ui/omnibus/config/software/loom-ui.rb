name "loom-ui"
version "0.1.1"

dependency "nodejs"

#source :git => "git://github.com/continuuity/loom.git"
#source :git => "/var/tmp/loom"

#relative_path 'ui'

build do
  command "cp -pPR /var/tmp/loom/ui/* ."
  command "mkdir -p bin"
  command "#{install_dir}/embedded/bin/node tools/r.js -o tools/build.js"
  command "cp /var/tmp/loom/bin/loom-ui.sh bin"
  command "sed -e 's/APP_NAME/loom-ui/g' -e 's/SVC_NAME/ui/g' /var/tmp/loom/bin/loom-service > bin/init-loom-ui"
  command "chmod a+x bin/*"
  command "mkdir -p #{install_dir}/etc/logrotate.d"
  command "cp -pP /var/tmp/loom/distribution/etc/logrotate.d/loom-ui #{install_dir}/etc/logrotate.d"
  command "rm -rf omnibus build*"
  command "cp /var/cache/omnibus/src/node*/LICENSE LICENSE.node"
  command "cp -pPR * #{install_dir}"
end
