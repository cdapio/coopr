# iptables-ng Cookbook

[![Build Status](https://travis-ci.org/chr4-cookbooks/iptables-ng.svg?branch=master)](https://travis-ci.org/chr4-cookbooks/iptables-ng)

This cookbook maintains and installs iptables and ip6tables rules, trying to keep as close to the way the used distribution maintains their rules.

Contrary to other iptables cookbooks, this cookbook installs iptables and maintains rules using the distributions default configuration files and services (for Debian and Ubuntu, iptables-persistent is used). If the distribution has no service for iptables, it falls back to iptables-restore.

It provides LWRPs as well as recipes which can handle iptables rules set in the nodes attributes.

It uses the directory ```/etc/iptables.d``` to store and maintain its rules. I'm trying to be as compatible as much as possible to all distributions out there.

## Requirements

The following distribution are best supported, but as this recipe falls back to a generic iptables restore script in case the system is unknown, it should work with every linux distribution supporting iptables.

* Ubuntu 10.04, 12.04, 14.04, 14.10
* Debian 7 (6 should work, too)
* RHEL 5.9, 6.x, 7.x
* Gentoo
* Archlinux

No external dependencies. Just add this line to your ```metadata.rb``` and you're good to go!

```ruby
depends 'iptables-ng'
```


## Attributes

### General configuration (services, paths)

While iptables-ng tries to automatically determine the correct settings and defaults for your distribution, it might be necessary to adapt them in certian cases. You can configure the behaviour of iptables-ng using the following attributes:

```ruby
# The ip versions to manage iptables for
node['iptables-ng']['enabled_ip_versions'] = [4, 6]

# Which tables to manage:
# When using a containered setup (OpenVZ, Docker, LXC) it might might be
# necessary to remove the "nat" and "raw" tables.
node['iptables-ng']['enabled_tables'] = %w(nat filter mangle raw)

# An array of packages to install.
# This should install iptables and ip6tables,
# as well as a system service that takes care of reloading the rules
# On Debian and Ubuntu, iptables-persistent is used by default.
node['iptables-ng']['packages'] = %w(iptables)

# The name of the service that will be used to restart iptables
# By default, the system service of your distribution is used, so don't worry about it unless you
# have special requirements. If iptables-ng can't figure out the default service to use or these
# attributes are set to nil, iptables-ng will fall back to "iptables-restore"
node['iptables-ng']['service_ipv4'] = 'iptables-persistent'
node['iptables-ng']['service_ipv6'] = 'iptables-persistent'

# The location were the iptables-restore script will be written to
node['iptables-ng']['script_ipv4'] = '/etc/iptables/rules.v4'
node['iptables-ng']['script_ipv6'] = '/etc/iptables/rules.v6'
```

### Rule configuration

The use of the LWRPs is recommended, but iptables-ng can be configured using attributes only.

You can set the default policies of a chain like this

```ruby
node['iptables-ng']['rules']['filter']['INPUT']['default'] = 'DROP [0:0]'
```

And also add rules for a chain (this example allows SSH)

```ruby
node['iptables-ng']['rules']['filter']['INPUT']['ssh']['rule'] = '--protocol tcp --dport 22 --match state --state NEW --jump ACCEPT'
```

You can prioritize your rules, too. This example will make sure that the 'ssh' rule is created before the 'http' rule

```ruby
node['iptables-ng']['rules']['filter']['INPUT']['10-ssh']['rule'] = 'this rule is first'
node['iptables-ng']['rules']['filter']['INPUT']['90-http']['rule'] = 'this rule is applied later'
```

Also, it's possible to only apply a rule for a certian ip version.

```ruby
node['iptables-ng']['rules']['filter']['INPUT']['10-ssh']['rule'] = '--protocol tcp --source 1.2.3.4 --dport 22 --match state --state NEW --jump ACCEPT'
node['iptables-ng']['rules']['filter']['INPUT']['10-ssh']['ip_version'] = 4
```


# Recipes

## default

The default recipe calls the install recipe, and then configures all rules and policies given in the nodes attribute.

Example:

To allow only SSH for incoming connections, add this to your node configuration

```json
{
  "name": "example.com",
  "chef_environment": "_default",
  "normal": {
    "iptables-ng": {
      "rules": {
        "filter": {
          "INPUT": {
            "default": "DROP [0:0]",
            "ssh": {
              "rule": "--protocol tcp --dport 22 --match state --state NEW --jump ACCEPT"
            }
          }
        }
      }
    }
  },
  "run_list": [
    "recipe[iptables-ng]"
  ]
}
```

In case you need a rule for one specific ip version, you can set the "ip_version" attribute.

```json
"ssh": {
  "rule": "--protocol tcp --source 1.2.3.4 --dport 22 --match state --state NEW --jump ACCEPT",
  "ip_version": 4
}
```

## install

The installs recipe installs iptables packages, makes sure that ```/etc/iptables.d``` is created and sets all default policies to "ACCEPT", unless they are already configured.

On Debian and Ubuntu systems, it also removes the "ufw" package, as it might interferre with this cookbook.


# Providers

It's recommended to configure iptables-ng using LWRPs in your (wrapper) cookbook.

All providers take care that iptables is installed (they include the install recipe before running), so you can just use them without worrying whether everything is installed correctly.


## iptables\_ng\_chain

This provider creates chains and adds their default policies.

Example: Set the default policy of the filter INPUT chain to ACCEPT:

```ruby
iptables_ng_chain 'INPUT' do
  policy 'ACCEPT [0:0]'
end
```

Example: Create a custom chain:

```ruby
iptables_ng_chain 'MYCHAIN'
```

The following additional attributes are supported:

```ruby
iptables_ng_chain 'name' do
  chain  'INPUT'       # The chain to set the policy for (name_attribute)
  table  'filter'      # The table to use (defaults to 'filter')
  policy 'DROP [0:0]'  # The policy to use (defaults to 'ACCEPT [0:0]' for
                       # build-in chains, to '- [0:0]' for custom ones

  action :create       # Supported actions: :create, :create_if_missing, :delete
                       # Default action: :create
end
```

## iptables\_ng\_rule

This provider adds iptables rules

Example: Allow SSH on the INPUT filter chain

```ruby
iptables_ng_rule 'ssh' do
  rule '--protocol tcp --dport 22 --match state --state NEW --jump ACCEPT'
end
```

The following additional attributes are supported:

```ruby
iptables_ng_rule 'custom' do
  name       'my-rule'    # Name of the rule. Use "xx-" to prioritize rules.
  chain      'INPUT'      # Chain to use. Defaults to 'INPUT'
  table      'filter'     # Table to use. Defaults to 'filter'
  ip_version 4            # Integer or Array of IP versions to create the rules for.
                          # Defaults to node['iptables-ng']['enabled_ip_versions']
  rule       '-j ACCEPT'  # String or Array containing the rule(s). (Required)

  action :create          # Supported actions: :create, :create_if_missing, :delete
                          # Default action: :create
end
```

Example: Allow HTTP and HTTPS for a specific IP range only

```ruby
iptables_ng_rule 'ssh' do
  rule ['--source 192.168.1.0/24 --protocol tcp --dport 80 --match state --state NEW --jump ACCEPT',
        '--source 192.168.1.0/24 --protocol tcp --dport 443 --match state --state NEW --jump ACCEPT']

  # As the source specified above is ipv4, this rule cannot be applied to ip6tables.
  # Therefore, setting ip_version to 4
  ip_version 4
end
```

Example: Use the same rule for an array of IPs

```ruby
ips = %w(10.10.10.1 123.123.123.123 192.168.1.0/24)

iptables_ng_rule 'multiple_source_addresses' do
  rule ips.map { |ip| "--source #{ip} --jump ACCEPT" }

  # As the source specified above is ipv4, this rule cannot be applied to ip6tables.
  # Therefore, setting ip_version to 4
  ip_version 4
end
```


# Known issues

There are some issues with systemd support on Fedora systems. Also it might be required to install iptables-service on newer Fedora machines.
Due to this issues, the tests for Fedora were removed until they are resolved.
Furthermore, due to the lack of Opscode kitchen boxes, there are not tests for Archlinux.


# Contributing

You fixed a bug, or added a new feature? Yippie!

1. Fork the repository on Github
2. Create a named feature branch (like `add_component_x`)
3. Write you change
4. Write tests for your change (if applicable)
5. Run the tests, ensuring they all pass
6. Submit a Pull Request using Github

Contributions of any sort are very welcome!

# License and Authors

Authors: Chris Aumann

Contributors: Dan Fruehauf, Nathan Williams, Christian Graf
