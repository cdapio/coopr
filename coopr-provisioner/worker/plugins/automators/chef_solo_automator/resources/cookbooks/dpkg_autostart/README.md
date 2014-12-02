# Dpkg Autostart

`dpkg` likes to start services after installation as the Debian convention
dictates. This can be annoying and problematic, especially when you don't
want services to be immediately started before custom configuration can
be applied. Instead of disabling all services from auto starting on installation
as the `dpkg_deb_unautostart` cookbook does, lets allow specifc services
to be targeted.

## Usage

### LWRP

Include the cookbook as a dependency in your metadata:

```ruby
depends 'dpkg_autostart'
```

Then, within your recipe, disable the service with the LWRP:

```ruby
dpkg_autostart 'mysql-server' do
  allow false
end
```

That's it. When `dpkg` attempts to start the service after install (generally
via `apt`) it will be instructed not to. This allows the proper configuration
files to be generated, and the service to be started after everything is
ready.

### Attribute

You can also provide a list of services to disable via node attribute. Add
the default recipe to the run list:

```ruby
run_list 'recipe[dpkg_autostart]'
```

and set the services you want to restrict from auto starting:

```ruby
node[:dpkg_autostart][:disabled_services] = ['mysql-server', 'apache2']
```

# Info
* Repository: https://github.com/hw-cookbooks/dpkg_autostart
* IRC: Freenode @ #heavywater

## Related
* deb_pkg_unautostart: http://ckbk.it/deb_pkg_unautostart