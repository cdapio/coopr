require 'spec_helper'
require 'json'

describe Provider do

  response = IO.read("#{File.dirname(__FILE__)}/task.json")

  # Set these up once
  before :all do
    %w(create confirm delete).each do |taskname|
      instance_variable_set("@task_#{taskname}", JSON.parse(response.gsub('BOOTSTRAP', taskname)))
      instance_variable_set("@provider_#{taskname}", Provider.new(instance_variable_get("@task_#{taskname}")))
    end
  end

  %w(create confirm delete).each do |taskname|
    @task = instance_variable_get("@task_#{taskname}")
    context "when taskName is #{taskname}" do
      describe '#new' do
        it "creates an instance of Provider" do
          expect(instance_variable_get("@provider_#{taskname}")).to be_an_instance_of Provider
        end
        it "creates task instance variable" do
          expect(instance_variable_get("@provider_#{taskname}").task).to eql instance_variable_get("@task_#{taskname}")
        end
        it "creates empty result hash" do
          expect(instance_variable_get("@provider_#{taskname}").result).to be_empty
        end
      end
    end
  end
end
