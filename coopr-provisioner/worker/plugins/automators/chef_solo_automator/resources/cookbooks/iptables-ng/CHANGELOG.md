iptables-ng CHANGELOG
=====================

This file is used to list changes made in each version of the iptables-ng cookbook.

2.2.2
-----

- Fix an issue with init-script name on Ubuntu >= 14.10 (was renamed to netfilter-persistent)

2.2.1
-----

- Add support for RHEL 7 compatible distributions


2.2.0
-----

- Add support for `node['iptables-ng']['enabled_tables']`


2.1.1
-----

- Fix an issue with `node['iptables-ng']['enabled_ip_versions']`, Thanks [Bob Ziuchkovski](https://github.com/ziuchkovski)
- Add Travis with rubocup and foodcritic checks

2.1.0
-----

- Add rubocup
- Add attribute `node['iptables-ng']['enabled_ip_versions']`


2.0.0
-----

- Support custom chains
- Rename/Migrate iptables\_ng\_policy provider to iptables\_ng\_chain

1.1.1
-----

- Fixes duplicate resource name warnings [CHEF-3694], Thanks [James FitzGibbon](http://github.com/jf647)

1.1.0
-----

- Support for ip\_version parameter in attributes. See README for details.

  If you use attributes to configure iptables\_ng, you need to migrate

  ```node['iptables-ng']['rules']['filter']['INPUT']['rej'] = 'myrule'```

  to

  ```node['iptables-ng']['rules']['filter']['INPUT']['rej']['rule'] = 'myrule'```


1.0.0
-----
- [Chris Aumann] - Initial release of iptables-ng
