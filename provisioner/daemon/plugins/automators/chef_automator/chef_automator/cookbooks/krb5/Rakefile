#!/usr/bin/env rake

# chefspec task against spec/*_spec.rb
require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new(:chefspec)

# foodcritic rake task
desc 'Foodcritic linter'
task :foodcritic do
  sh 'foodcritic -f correctness .'
end

# rubocop rake task
desc 'Ruby style guide linter'
task :rubocop do
  sh 'rubocop'
end

# rubocop jenkins rake task
desc 'Ruby style guide - checkformat output'
task :rubocop_checkformat do
  sh 'rubocop --require rubocop/formatter/checkstyle_formatter --format Rubocop::Formatter::CheckstyleFormatter > checkstyle.xml'
end

# test-kitchen task
begin
  require 'kitchen/rake_tasks'
  Kitchen::RakeTasks.new
rescue LoadError
  puts '>>>>> Kitchen gem not loaded, omitting tasks' unless ENV['CI']
end

# Deploy task
desc 'Deploy to chef server and pin to environment'
task :deploy do
  sh 'berks upload'
  sh 'berks apply ci'
end

# default tasks are quick, commit tests
task :default => ['foodcritic', 'rubocop', 'chefspec']

# jenkins tasks format for metric tracking
task :jenkins => ['foodcritic', 'rubocop_checkformat', 'chefspec'] 
