# hadoop wrapper cookbook

# Description

This cookbook is a wrapper cookbook for the [Hadoop cookbook](https://github.com/continuuity/hadoop_cookbook).  It is a part of [Continuuity Loom](https://github.com/continuuity/loom), which is a general purpose tool that can spin up several types of clusters including Hadoop.  This cookbook provides several initialization recipes for Hadoop components.  It does not actually start any of the hadoop services.  This can be done by wrapping the service resources in the underlying [Hadoop cookbook](https://github.com/continuuity/hadoop_cookbook), for example:
```ruby
    ruby_block "start namenode" do
      block do
        resources("hadoop-hdfs-namenode").run_action(:start)
      end 
```

Additional information can be found in the [Hadoop cookbook wiki](https://github.com/continuuity/hadoop_cookbook/wiki/Wrapping-this-cookbook).


# Requirements

* Chef 11.4.0+
* CentOS 6.4+
* Ubuntu 12.04+


# Cookbook Dependencies

apt
yum
java
hadoop
krb5_utils (from https://github.com/continuuity/krb5_utils_cookbook)

# Attributes

There are no attributes specific to this cookbook, however we set many default attributes for the underlying cookbooks in order to have a reasonably configured Hadoop cluster.  Be sure to look at the attributes files and override as desired.


# Usage

Include the relevant recipes in your run-list.



