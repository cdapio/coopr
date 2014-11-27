source 'https://rubygems.org'

gem 'berkshelf', '~> 3.1.2'

group :unit do
  gem 'foodcritic',       '~> 3.0'
  gem 'rubocop',          '~> 0.21.0'
  gem 'chefspec',         '~> 3.4.0'
end

group :integration do
  gem 'test-kitchen',    '~> 1.2'
  gem 'kitchen-vagrant', :require => false
  gem 'kitchen-docker', :require => false
end

group :release do
  gem 'rspec_junit_formatter'
  gem 'rubocop-checkstyle_formatter'
end

group :development do
  gem 'guard',            '~> 1.8'
  gem 'guard-rubocop',    '~> 0.2'
  gem 'guard-foodcritic', '~> 1.0'
  gem 'guard-kitchen',    '~> 0.0'
  gem 'guard-rspec',      '~> 3.0'
  gem 'rb-fsevent', :require => false
  gem 'rb-inotify', :require => false
  gem 'terminal-notifier-guard', :require => false
end
