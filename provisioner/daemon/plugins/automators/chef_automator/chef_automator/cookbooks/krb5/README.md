krb5 Cookbook
=============

[![Build Status](https://secure.travis-ci.org/atomic-penguin/cookbook-krb5.svg?branch=master)](http://travis-ci.org/atomic-penguin/cookbook-krb5)


Description
-----------

Installs and configures Kerberos version 5 authentication modules
on RedHat and Debian family systems.

Requirements
------------

Requires some PAM configuration script such as pam-auth-update on Debian
family systems, or authconfig on Redhat family systems.  Best effort is
made to use one of these two tools based on detected platform.

You can override krb5['authconfig'] with an execute command, as a string.
Which should configure PAM to use Kerberos on other systems.

You really need to have time synchronized within 5 minutes of your domain
controllers, or key distribution centers.  Therefore the recipe depends
on the Opscode NTP cookbook.  If you have another method of keeping accurate
clocks, change the metadata according to your needs.

Attributes
----------

This cookbook has changed the attribute format, but is backwards compatible with `0.2.0` attributes. The new format used for template variables consists of `krb5[file][section][key] = 'value'` where file is one of `krb5_conf`, `kadm5_acl`, or `kdc_conf`.

## Client / Libs

 * `krb5['client']['packages']` - Packages and libraries needed for Kerberos v5 authentication, detected for Redhat/Debian family systems.
 * `krb5['client']['authconfig']` - Configuration script for PAM, detected for RedHat and Debian family systems.

### Section: logging

 * `krb5['krb5_conf']['logging']['default']` - Default log location.  Default, 'FILE:/var/log/krb5libs.log'

### Section: libdefaults

 * `krb5['krb5_conf']['libdefaults']['default_realm']` - The default realm, defaults to OHAI's domain attribute.
 * `krb5['krb5_conf']['libdefaults']['dns_lookup_kdc']` - Set to true if you have SRV records for KDC discovery.  Default is true.
 * `krb5['krb5_conf']['libdefaults']['dns_lookup_realm']` - Set to true if you have TXT records for realm discovery.  Default is false.
 * `krb5['krb5_conf']['libdefaults']['forwardable']` - Set to true to make initial credentials forwardable.  Default is true.
 * `krb5['krb5_conf']['libdefaults']['renew_lifetime']` - Default renewable ticket lifetime.  Default is `24h`.
 * `krb5['krb5_conf']['libdefaults']['ticket_lifetime']` - Default ticket lifetime.  Default is `24h`.

### Section: realms

 * `krb5['krb5_conf']['realms']['default_realm']` - The default realm, defaults to `krb5['krb5_conf']['libdefaults']['default_realm']`
 * `krb5['krb5_conf']['realms']['default_realm_kdcs']` - Array of Kerberos servers for default realm.  Default is empty.
 * `krb5['krb5_conf']['realms']['default_realm_admin_server']` - Address of Kerberos admin server.  Defaults to empty.
 * `krb5['krb5_conf']['realms']['realms']` - Array of all realms, including the default.  Defaults to OHAI's domain attribute.

### Section: appdefaults

 * `krb5['krb5_conf']['appdefaults']['pam']['debug']` = Set to true to enable PAM/Kerberos debugging.  Defaults to false.
 * `krb5['krb5_conf']['appdefaults']['pam']['forwardable']` - Instruct PAM to create forwardable tickets.  Defaults to `krb5['krb5_conf']['libdefaults']['forwardable']`
 * `krb5['krb5_conf']['appdefaults']['pam']['renew_lifetime']` - Defaults to `krb5['krb5_conf']['libdefaults']['renew_lifetime']`
 * `krb5['krb5_conf']['appdefaults']['pam']['ticket_lifetime']` - Defaults to `krb5['krb5_conf']['libdefaults']['ticket_lifetime']`
 * `krb5['krb5_conf']['appdefaults']['pam']['krb4_convert']` - Set to true to use the Kerberos conversion daemon to get V4 tickets.  Default is false.

## Kerberos Admin Server (kadmind)

 * `krb5['kadmin']['packages']` - Packages for Kerberos Admin Server, detected on Redhat/Debian family systems.
 * `krb5['master_password']` - Master password for Kerberos database.  Default is `password`. (Please, change this!)
 * `krb5['admin_principal']` - Principal to create for administration.  Default is `admin/admin`.
 * `krb5['admin_password']` - Password for admin principal.  Default is `password`. (Please, change this!)

### Section: logging

 * `krb5['krb5_conf']['logging']['admin_server']` - Kerberos Admin Server log location.  Default, 'FILE:/var/log/kadmind.log'

### kadm5.acl

  * `krb5['kadm5_acl'][principal]` - Sets up ACLs for `principal`.  Default is `"*/admin@#{node['krb5']['krb5_conf']['libdefaults']['default_realm'].upcase}" => ['*']`

## KDC and kdc.conf

 * `krb5['kdc']['packages']` - Packages needed for a KDC, detected for Redhat/Debian family systems.

### Section: logging

 * `krb5['krb5_conf']['logging']['kdc']` - KDC log location.  Default, 'FILE:/var/log/krb5kdc.log'

### Section: kdcdefaults

 * `krb5['kdc_conf']['kdcdefaults']['kdc_ports']` - Set KDC listen ports.  Default is `88`.

### Section: realms

 * `krb5['kdc_conf']['realms'][realm]['acl_file']` - Location of kadmind ACL file for `realm`.  Defaults to `default_realm`.
 * `krb5['kdc_conf']['realms'][realm]['admin_keytab']` - Location of admin keytab file for `realm`.  Defaults to `default_realm`.

Usage
-----

Here are two example roles to be used with this recipe.  The first, is
a single realm configuration, using the OHAI domain attribute for the realm.

```
name "krb5_domain"
description "Configures Kerberos 5 Authentication for domain realm"
override_attributes "krb5" => {
  "default_realm_kdcs" => [
    "kdc1.example.com",
    "kdc2.example.com",
    "kdc3.example.com"
  ]
}
run_list "recipe[krb5]"
```

The second example is a role for multiple Kerberos realms.


```
name "krb5_multirealm"
description "Configures Kerberos 5 Authentication for example.com and example.org realm"
override_attributes "krb5" => {
  "default_realm" = > "example.com",
  "realms" => [ 
    "example.com",
    "example.org"
  ],
  "default_realm_kdcs" => [
    "kdc1.example.com",
    "kdc2.example.com",
    "kdc3.example.com"
  ],
  "lookup_kdcs" => "true"
}
run_list "recipe[krb5]"
```

License and Authors
-------------------

Author:: Eric G. Wolfe
Author:: Chris Gianelloni
Copyright:: 2012-2014, Eric G. Wolfe
Copyright:: 2014, Continuuity, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License
