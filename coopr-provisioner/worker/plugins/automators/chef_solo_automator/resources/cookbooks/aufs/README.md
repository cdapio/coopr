# chef-aufs [![Build Status](https://secure.travis-ci.org/bflad/chef-aufs.png?branch=master)](http://travis-ci.org/bflad/chef-aufs)

## Description

Installs/Configures AUFS.

## Requirements

### Chef

* Chef 11+

### Platforms

* Ubuntu 12.04, 12.10, 13.04, 13.10

### Cookbooks

[Opscode Cookbooks](https://github.com/opscode-cookbooks/)

* [apt](https://github.com/opscode-cookbooks/apt)

Third-Party Cookbooks

* [modules](https://github.com/Youscribe/modules-cookbook)

## Attributes

None currently.

## Recipes

* `recipe[aufs]` Installs/Configures AUFS
* `recipe[aufs::module]` Loads AUFS Linux module
* `recipe[aufs::package]` Installs AUFS via package

## Usage

### Default Installation

* Add `recipe[aufs]` to your node's run list

## Testing and Development

* Quickly testing with Vagrant: [VAGRANT.md](VAGRANT.md)
* Full development and testing workflow with Test Kitchen and friends: [TESTING.md](TESTING.md)

## Contributing

Please see contributing information in: [CONTRIBUTING.md](CONTRIBUTING.md)

## Maintainers

* Brian Flad (<bflad417@gmail.com>)

## License

Please see licensing information in: [LICENSE](LICENSE)
