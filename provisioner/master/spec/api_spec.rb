# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


require File.expand_path '../spec_helper.rb', __FILE__
require File.expand_path '../../lib/provisioner/api.rb', __FILE__
require File.expand_path '../../lib/provisioner/provisioner.rb', __FILE__

describe 'Sinatra API' do
  include Rack::Test::Methods
  def app
    Coopr::Provisioner::Api
  end

  Coopr::Provisioner::Api.set :provisioner, Coopr::Provisioner.new({}, Coopr::Config.new({}))

  it 'should serve status endpoint' do
    get '/status'
    puts last_response.inspect
    expect(last_response).to be_ok
  end

  it 'should serve heartbeat endpoint' do
    get '/heartbeat'
    puts last_response.inspect
    expect(last_response).to be_ok
  end

  it 'can do tenant operations' do
    post 'v2/tenants', {:id => "test_tenant", :workers => 0}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    put 'v2/tenants/test_tenant', {:id => "test_tenant", :workers => 1}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    delete 'v2/tenants/test_tenant', "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
  end

  it 'can delete a tenant with no workers' do
    post 'v2/tenants', {:id => "test_tenant2", :workers => 0}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    delete 'v2/tenants/test_tenant2', "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
  end

end
