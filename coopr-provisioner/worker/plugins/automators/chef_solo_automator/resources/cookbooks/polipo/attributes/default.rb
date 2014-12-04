default[:polipo][:install][:action] = :install
default[:polipo][:install][:version] = nil

default[:polipo][:directories][:config] = '/etc/polipo'

default[:polipo][:config][:allowed_clients] = '127.0.0.1'

default[:polipo][:options][:method] = ['any']

default[:polipo][:forbidden] = []
