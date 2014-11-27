# chef-device-mapper [![Build Status](https://secure.travis-ci.org/bflad/chef-device-mapper.png?branch=master)](http://travis-ci.org/bflad/chef-device-mapper)

## Description

Installs/Configures device-mapper.

## Requirements

### Chef

* Chef 11+

### Platforms

* CentOS 6
* Debian 7
* Fedora 19, 20
* Oracle 6
* RHEL 6
* Ubuntu 12.04, 12.10, 13.04, 13.10

### Cookbooks

[Opscode Cookbooks](https://github.com/opscode-cookbooks/)

* [apt](https://github.com/opscode-cookbooks/apt)

Third-Party Cookbooks

* [modules](https://github.com/Youscribe/modules-cookbook)

## Attributes

These attributes are under the `node['device-mapper']` namespace.

Attribute | Description | Type | Default
----------|-------------|------|--------
packages | Packages for installation | Array of Strings | auto-detected (see attributes/default.rb)

## Recipes

* `recipe[device-mapper]` Installs/Configures device-mapper
* `recipe[device-mapper::module]` Loads device-mapper Linux module
* `recipe[device-mapper::package]` Installs device-mapper via package

## Usage

### Default Installation

* Add `recipe[device-mapper]` to your node's run list

## Testing and Development

* Quickly testing with Vagrant: [VAGRANT.md](VAGRANT.md)
* Full development and testing workflow with Test Kitchen and friends: [TESTING.md](TESTING.md)

## Contributing

Please see contributing information in: [CONTRIBUTING.md](CONTRIBUTING.md)

## Maintainers

* Brian Flad (<bflad417@gmail.com>)

## License

Please see licensing information in: [LICENSE](LICENSE)
