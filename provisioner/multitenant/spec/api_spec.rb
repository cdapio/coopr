require File.expand_path '../spec_helper.rb', __FILE__
require File.expand_path '../../lib/provisioner/api.rb', __FILE__
require File.expand_path '../../lib/provisioner/provisioner.rb', __FILE__

describe 'Sinatra API' do
  include Rack::Test::Methods
  def app
    Loom::Provisioner::Api
  end

  Loom::Provisioner::Api.set :provisioner, Loom::Provisioner.new({})

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
    post 'v1/tenants', {:id => "test_tenant", :workers => 0}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    put 'v1/tenants/test_tenant', {:id => "test_tenant", :workers => 1}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    delete 'v1/tenants/test_tenant', "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
  end

  it 'can delete a tenant with no workers' do
    post 'v1/tenants', {:id => "test_tenant", :workers => 0}.to_json, "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
    delete 'v1/tenants/test_tenant', "CONTENT_TYPE" => "application/json"
    expect(last_response).to be_ok
  end

end
