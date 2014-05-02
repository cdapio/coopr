# Java
default['java']['install_flavor'] = 'oracle'
default['java']['jdk_version'] = 6
default['java']['oracle']['accept_oracle_download_terms'] = true

# Hadoop
# core-site.xml
default['hadoop']['core_site']['hadoop.tmp.dir'] = '/hadoop'
# hdfs-site.xml
default['hadoop']['hdfs_site']['dfs.datanode.max.transfer.threads'] = '4096'
# mapred-site.xml
default['hadoop']['mapred_site']['mapreduce.framework.name'] = 'yarn'
# yarn-site.xml
default['hadoop']['yarn_site']['yarn.log-aggregation-enable'] = 'true'
default['hadoop']['yarn_site']['yarn.scheduler.minimum-allocation-mb'] = '512'
default['hadoop']['yarn_site']['yarn.nodemanager.vmem-check-enabled'] = 'false'
default['hadoop']['yarn_site']['yarn.nodemanager.vmem-pmem-ratio'] = '5.1'
default['hadoop']['yarn_site']['yarn.nodemanager.delete.debug-delay-sec'] = '86400'
default['hadoop']['yarn_site']['yarn.nodemanager.resource.memory-mb'] = (node[:memory][:total].to_i / 1024) / 2
# Do the right thing, based on distribution
case node['hadoop']['distribution']
when 'cdh'
  # CDH4 doesn't have https://issues.apache.org/jira/browse/YARN-9 fixed
  default['hadoop']['yarn_site']['yarn.application.classpath'] = '$HADOOP_CONF_DIR, $HADOOP_COMMON_HOME/*, $HADOOP_COMMON_HOME/lib/*, $HADOOP_HDFS_HOME/*, $HADOOP_HDFS_HOME/lib/*, $HADOOP_MAPRED_HOME/*, $HADOOP_MAPRED_HOME/lib/*, $YARN_HOME/*, $YARN_HOME/lib/*'
  # CDH4 doesn't have https://issues.apache.org/jira/browse/YARN-1229 fixed
  default['hadoop']['yarn_site']['yarn.nodemanager.aux-services'] = 'mapreduce.shuffle'
  default['hadoop']['yarn_site']['yarn.nodemanager.aux-services.mapreduce.shuffle.class'] = 'org.apache.hadoop.mapred.ShuffleHandler'
else
  default['hadoop']['yarn_site']['yarn.application.classpath'] = '$HADOOP_CONF_DIR, $HADOOP_COMMON_HOME/*, $HADOOP_COMMON_HOME/lib/*, $HADOOP_HDFS_HOME/*, $HADOOP_HDFS_HOME/lib/*, $HADOOP_MAPRED_HOME/*, $HADOOP_MAPRED_HOME/lib/*, $HADOOP_YARN_HOME/*, $HADOOP_YARN_HOME/lib/*'
  # Fix for: org.apache.hadoop.yarn.exceptions.InvalidAuxServiceException: The auxService:mapreduce_shuffle does not exist
  default['hadoop']['yarn_site']['yarn.nodemanager.aux-services'] = 'mapreduce_shuffle'
  default['hadoop']['yarn_site']['yarn.nodemanager.aux-services.mapreduce_shuffle.class'] = 'org.apache.hadoop.mapred.ShuffleHandler'
end
# Set FIFO scheduler
default['hadoop']['yarn_site']['yarn.resourcemanager.scheduler.class'] = 'org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler'
# hadoop-metrics.properties
default['hadoop']['hadoop_metrics']['dfs.class'] = 'org.apache.hadoop.metrics.spi.NullContextWithUpdateThread'
default['hadoop']['hadoop_metrics']['dfs.period'] = '60'
default['hadoop']['hadoop_metrics']['mapred.class'] = 'org.apache.hadoop.metrics.spi.NullContextWithUpdateThread'
default['hadoop']['hadoop_metrics']['mapred.period'] = '60'
default['hadoop']['hadoop_metrics']['rpc.class'] = 'org.apache.hadoop.metrics.spi.NullContextWithUpdateThread'
default['hadoop']['hadoop_metrics']['rpc.period'] = '60'
default['hadoop']['hadoop_metrics']['ugi.class'] = 'org.apache.hadoop.metrics.spi.NullContextWithUpdateThread'
default['hadoop']['hadoop_metrics']['ugi.period'] = '60'
# hadoop-env.sh
# Enable JMX
default['hadoop']['hadoop_env']['hadoop_jmx_base'] = '-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false'
default['hadoop']['hadoop_env']['hadoop_namenode_opts'] = '$HADOOP_JMX_BASE -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8004'
default['hadoop']['hadoop_env']['hadoop_secondarynamenode_opts'] = '$HADOOP_JMX_BASE -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8005'
default['hadoop']['hadoop_env']['hadoop_datanode_opts'] = '$HADOOP_JMX_BASE -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8006'
default['hadoop']['hadoop_env']['hadoop_mapred_home'] = '/usr/lib/hadoop-mapreduce'
# yarn-env.sh
default['hadoop']['yarn_env']['yarn_opts'] = '-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false'
default['hadoop']['yarn_env']['yarn_resourcemanager_opts'] = '$YARN_RESOURCEMANAGER_OPTS -Dcom.sun.management.jmxremote.port=8008'
default['hadoop']['yarn_env']['yarn_nodemanager_opts'] = '$YARN_NODEMANAGER_OPTS -Dcom.sun.management.jmxremote.port=8009'

# HBase
# hbase-site.xml configs
default['hbase']['hbase_site']['hbase.cluster.distributed'] = 'true'
default['hbase']['hbase_site']['hbase.defaults.for.version.skip'] = 'false'
default['hbase']['hbase_site']['hbase.regionserver.handler.count'] = '100'
