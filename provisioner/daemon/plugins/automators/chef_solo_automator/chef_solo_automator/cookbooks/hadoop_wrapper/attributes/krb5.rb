# Hadoop
if node['hadoop'].key?('core_site') && node['hadoop']['core_site'].key?('hadoop.security.authorization') &&
  node['hadoop']['core_site'].key?('hadoop.security.authentication') &&
  node['hadoop']['core_site']['hadoop.security.authorization'].to_s == 'true' &&
  node['hadoop']['core_site']['hadoop.security.authentication'] == 'kerberos'

  include_attribute 'krb5'
  include_attribute 'krb5_utils'

  # container-executor.cfg
  default['hadoop']['container_executor']['banned.users'] = 'hdfs,yarn,mapred,bin'
  default['hadoop']['container_executor']['min.user.id'] = 500
  default['hadoop']['container_executor']['yarn.nodemanager.linux-container-executor.group'] = 'yarn'
  default['hadoop']['container_executor']['yarn.nodemanager.local-dirs'] =
    if node['hadoop'].key?('yarn_site') && node['hadoop']['yarn_site'].key?('yarn.nodemanager.local-dirs')
      node['hadoop']['yarn_site']['yarn.nodemanager.local-dirs']
    elsif node['hadoop'].key? 'hadoop.tmp.dir'
      "#{node['hadoop']['hadoop.tmp.dir']}/nm-local-dir"
    else
      '/tmp/hadoop-yarn/nm-local-dir'
    end
  default['hadoop']['container_executor']['yarn.nodemanager.log-dirs'] = '/var/log/hadoop-yarn/userlogs'

  # hadoop-env.sh
  default['hadoop']['hadoop_env']['jsvc_home'] =
    if node['platform_family'] == 'debian'
      '/usr/lib/bigtop-utils'
    elsif node['platform_family'] == 'rhel'
      '/usr/libexec/bigtop-utils'
    end
  default['hadoop']['hadoop_env']['hadoop_secure_dn_user'] = 'hdfs'
  default['hadoop']['hadoop_env']['hadoop_secure_dn_pid_dir'] = '/var/run/hadoop-hdfs'
  default['hadoop']['hadoop_env']['hadoop_secure_dn_log_dir'] = '/var/log/hadoop-hdfs'

  # hdfs-site.xml
  default['hadoop']['hdfs_site']['dfs.block.access.token.enable'] = 'true'
  default['hadoop']['hdfs_site']['dfs.datanode.kerberos.principal'] = "hdfs/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.namenode.kerberos.principal'] = "hdfs/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.secondary.namenode.kerberos.principal'] = "hdfs/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.web.authentication.kerberos.principal'] = "HTTP/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.namenode.kerberos.internal.spnego.principal'] = "HTTP/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.secondary.namenode.kerberos.internal.spnego.principal'] = "HTTP/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['hdfs_site']['dfs.datanode.keytab.file'] = "#{node['krb5_utils']['keytabs_dir']}/hdfs.service.keytab"
  default['hadoop']['hdfs_site']['dfs.namenode.keytab.file'] = "#{node['krb5_utils']['keytabs_dir']}/hdfs.service.keytab"
  default['hadoop']['hdfs_site']['dfs.secondary.namenode.keytab.file'] = "#{node['krb5_utils']['keytabs_dir']}/hdfs.service.keytab"
  default['hadoop']['hdfs_site']['dfs.datanode.address'] = '0.0.0.0:1004'
  default['hadoop']['hdfs_site']['dfs.datanode.http.address'] = '0.0.0.0:1006'

  # yarn-site.xml
  default['hadoop']['yarn_site']['yarn.resourcemanager.keytab'] = "#{node['krb5_utils']['keytabs_dir']}/yarn.service.keytab"
  default['hadoop']['yarn_site']['yarn.nodemanager.keytab'] = "#{node['krb5_utils']['keytabs_dir']}/yarn.service.keytab"
  default['hadoop']['yarn_site']['yarn.resourcemanager.principal'] = "yarn/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['yarn_site']['yarn.nodemanager.principal'] = "yarn/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hadoop']['yarn_site']['yarn.nodemanager.container-executor.class'] = 'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor'
  default['hadoop']['yarn_site']['yarn.nodemanager.linux-container-executor.group'] = 'yarn'

end

# HBase
if node['hbase'].key?('hbase_site') && node['hbase']['hbase_site'].key?('hbase.security.authorization') &&
  node['hbase']['hbase_site'].key?('hbase.security.authentication') &&
  node['hbase']['hbase_site']['hbase.security.authorization'].to_s == 'true' &&
  node['hbase']['hbase_site']['hbase.security.authentication'] == 'kerberos'

  include_attribute 'krb5'
  include_attribute 'krb5_utils'

  # hbase-site.xml
  default['hbase']['hbase_site']['hbase.master.keytab.file'] = "#{node['krb5_utils']['keytabs_dir']}/hbase.service.keytab"
  default['hbase']['hbase_site']['hbase.regionserver.keytab.file'] = "#{node['krb5_utils']['keytabs_dir']}/hbase.service.keytab"
  default['hbase']['hbase_site']['hbase.master.kerberos.principal'] = "hbase/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hbase']['hbase_site']['hbase.regionserver.kerberos.principal'] = "hbase/_HOST@#{node['krb5']['krb5_conf']['realms']['default_realm'].upcase}"
  default['hbase']['hbase_site']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController'
  default['hbase']['hbase_site']['hbase.coprocessor.master.classes'] = 'org.apache.hadoop.hbase.security.access.AccessController'
  default['hbase']['hbase_site']['hbase.bulkload.staging.dir'] = '/tmp/hbase-staging'

end
