name "loom-server"
version "0.1.1"

#source :git => "git://github.com/continuuity/loom.git"
#relative_path 'server'

build do
  command "cp -pPR /var/tmp/loom/server/pom.xml /var/tmp/loom/server/src ."
  command "mkdir -p conf"
  command "cp -fpPR /var/tmp/loom/docs/licenses ."
  command "cp -fpPR /var/tmp/loom/config ."
  command "cp -fpPR /var/tmp/loom/distribution/etc/loom/conf.dist/* conf"
  command "/usr/local/maven-3.1.1/bin/mvn clean package assembly:single -DskipTests=true"
  command "mkdir -p bin"
  command "cp -f /var/tmp/loom/bin/loom-server.sh bin"
  command "sed -e 's/APP_NAME/loom-server/g' -e 's/SVC_NAME/server/g' /var/tmp/loom/bin/loom-service > bin/init-loom-server"
  command "chmod a+x bin/*"
  command "mkdir -p #{install_dir}/etc/logrotate.d"
  command "cp -f /var/tmp/loom/distribution/etc/logrotate.d/loom-server #{install_dir}/etc/logrotate.d"
  command "mkdir -p #{install_dir}/lib"
  command "cp -fpPR bin conf licenses config #{install_dir}"
  command "cp -fpPR target/loom-*jar-with-dependencies.jar #{install_dir}/lib"
end
