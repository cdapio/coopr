#!/usr/bin/env rake
# encoding: UTF-8

require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new(:rspec) do |t|
  t.pattern = 'master/spec/**/*_spec.rb,worker/spec/**/*_spec.rb'
end

# rubocop rake task
desc 'Ruby style guide linter'
task :rubocop do
  sh 'rubocop -D'
end

task :default => %w(rubocop rspec)
