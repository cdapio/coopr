require 'rest_client'
require 'openssl'

module Coopr
  class RestHelper

    attr_accessor :pem_path, :pem_pass

    def initialize(pem_path, pem_pass)
      @pem_path = pem_path
      @pem_pass = pem_pass
    end

    def get_resource(uri)      
      if pem_path.nil? || pem_path.empty?
        RestClient::Resource.new(uri)
      else
        pem = File.read(pem_path)
        RestClient::Resource.new(
            uri,
            :ssl_client_cert => OpenSSL::X509::Certificate.new(pem),
            :ssl_client_key => OpenSSL::PKey::RSA.new(pem, pem_pass)
        )
      end
    end

    def get(uri, headers={})
      resource = get_resource(uri)
      resource.get(headers)
    end

    def post(uri, payload, headers={})
      resource = get_resource(uri)
      resource.post(payload, headers)
    end

    def put(uri, payload, headers={})
      resource = get_resource(uri)
      resource.put(payload, headers)
    end

    def delete(uri, headers={})
      resource = get_resource(uri)
      resource.delete(headers)
    end

  end
end
