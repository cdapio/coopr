require 'spec_helper'
require 'json'

describe Automator do

  response = IO.read("#{File.dirname(__FILE__)}/task.json")

  # Set these up once
  before :all do
    %w(bootstrap install configure initialize start stop remove).each do |taskname|
      instance_variable_set("@task_#{taskname}", JSON.parse(response.gsub('BOOTSTRAP', taskname)))
      instance_variable_set("@automator_#{taskname}", Automator.new(instance_variable_get("@task_#{taskname}")))
    end
  end

  %w(bootstrap install configure initialize start stop remove).each do |taskname|
    @task = instance_variable_get("@task_#{taskname}")
    context "when taskName is #{taskname}" do
      describe '#new' do
        it 'creates an instance of Automator' do
          expect(instance_variable_get("@automator_#{taskname}")).to be_an_instance_of Automator
        end
        it 'creates task instance variable' do
          expect(instance_variable_get("@automator_#{taskname}").task).to eql instance_variable_get("@task_#{taskname}")
        end
        it 'creates empty result hash' do
          expect(instance_variable_get("@automator_#{taskname}").result).to be_empty
        end
      end

# TODO: fix this
# rubocop:disable CommentIndentation
# This does not work and I don't know why. Leaving it here until I can fix this. Do not remove.
#      describe '#runTask' do
#        it 'errors when run' do
#          expect(instance_variable_get("@automator_#{taskname}").runTask).to raise_error
#        end
#      end
# rubocop:enable CommentIndentation

    end
  end
end
