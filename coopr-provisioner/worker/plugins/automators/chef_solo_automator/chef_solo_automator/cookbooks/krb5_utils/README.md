# krb5_utils cookbook

[![Cookbook Version](http://img.shields.io/cookbook/v/krb5_utils.svg)](https://supermarket.getchef.com/cookbooks/krb5_utils)
[![Build Status](http://img.shields.io/travis/caskdata/krb5_utils_cookbook.svg)](http://travis-ci.org/caskdata/krb5_utils_cookbook)

# Requirements

This cookbook requires the `krb5` cookbook, version `1.0.4` or greater.

# Usage

This cookbook is intended for use in creating secure Hadoop clusters. As such, it will add the HTTP service
principal to all other service principals. This can be disabled by setting `krb5_utils['add_http_principal']` to `false`.

# Attributes

* `krb5_utils['admin_principal']` - Initial administrator principal, created on KDC. Default `admin/admin`
* `krb5_utils['admin_password']` - Password for `krb5_utils['admin_principal']`. Default `password`
* `krb5_utils['keytabs_dir']` - On-disk location for storing keytab files. Default `/etc/security/keytabs`
* `krb5_utils['krb5_service_keytabs']` - Hash of service keytabs to create. Default `{}`
* `krb5_utils['krb5_user_keytabs']` - Hash of user keytabs to create, in the same format as `krb5_utils['krb5_service_keytabs']`. Default `{}`

Hash format:
```ruby
{ 'HTTP' => { 'owner' => 'hdfs', 'group' => 'hadoop', 'mode' => '0640' } }
```

# License

Copyright © 2014 Cask Data, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
