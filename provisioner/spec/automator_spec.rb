require 'spec_helper'
require 'json'

describe Automator do

  response = '{"taskId":"00000001-001-0001","jobId":"00000001-001","clusterId":"00000001","taskName":"BOOTSTRAP","nodeId":"00000000-aaaa-1111-bbbb-123456789abc","config":{"cluster":{},"nodenum":"1000","hardwaretype":"large","flavor":"6","hostname":"test001-1000.local","image":"ffa476b1-9b14-46bd-99a8-862d1d94eb7a","imagetype":"ubuntu12","automators":["shell","chef"],"provider":{"name":"rackspace","description":"Rackspace Iaas Provider","providertype":"rackspace","provisioner":{"auth":{"rackspace_username":"username","rackspace_api_key":"123456789abcdef123456789abcdef00"}}},"providerid":"00000000-aaaa-1111-bbbb-123456789abc","ssh-auth":{"user":"root","password":"password"},"ipaddress":"192.168.0.1","nodes":{"00000000-aaaa-1111-bbbb-123456789abc":{"ipaddress":"192.168.0.1","hostname":"test001-1000.local","nodenum":"1000","automators":["shell","chef"]}}}}'

  # Set these up once
  before :all do
    %w(bootstrap install configure initialize start stop remove).each do |taskname|
      instance_variable_set("@task_#{taskname}", JSON.parse(response.to_str.gsub('BOOTSTRAP', taskname)))
      instance_variable_set("@automator_#{taskname}", Automator.new(instance_variable_get("@task_#{taskname}")))
    end
  end

  describe '#new' do
    %w(bootstrap install configure initialize start stop remove).each do |taskname|
      it "creates an instance of Automator for taskName=#{taskname}" do
        instance_variable_get("@automator_#{taskname}").should be_an_instance_of Automator
      end
      it "creates task instance variable for taskName=#{taskname}" do
        instance_variable_get("@automator_#{taskname}").task.should eql instance_variable_get("@task_#{taskname}")
      end
    end
  end
end
