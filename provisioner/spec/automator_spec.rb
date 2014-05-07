require 'spec_helper'
require 'json'

describe Automator do

  response = IO.read("#{File.dirname(__FILE__)}/task.json")

  # Set these up once
  before :all do
    %w(bootstrap install configure initialize start stop remove).each do |taskname|
      instance_variable_set("@task_#{taskname}", JSON.parse(response.to_str.gsub('BOOTSTRAP', taskname)))
      instance_variable_set("@automator_#{taskname}", Automator.new(instance_variable_get("@task_#{taskname}")))
    end
  end

  %w(bootstrap install configure initialize start stop remove).each do |taskname|
    context "when taskName is #{taskname}" do
      describe '#new' do
        it "creates an instance of Automator for taskName=#{taskname}" do
          instance_variable_get("@automator_#{taskname}").should be_an_instance_of Automator
        end
        it "creates task instance variable for taskName=#{taskname}" do
          instance_variable_get("@automator_#{taskname}").task.should eql instance_variable_get("@task_#{taskname}")
        end
      end
    end
  end
end
