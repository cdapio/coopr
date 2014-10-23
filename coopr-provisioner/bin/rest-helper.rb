#!/usr/bin/env ruby
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

require 'rest_client'

class RestHelper

  def self.get_resource(uri)
    pem_path = ENV['TRUST_CERT_PATH']
    pem_pass = ENV['TRUST_CERT_PASSWORD']
    if pem_path == nil
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

  def self.get(uri, headers={})
    resource = get_resource(uri)
    resource.get(headers)
  end

  def self.post(uri, payload, headers={})
    resource = get_resource(uri)
    resource.post(payload, headers)
  end

  def self.put(uri, payload, headers={})
    resource = get_resource(uri)
    resource.put(payload, headers)
  end

  def self.delete(uri, headers={})
    resource = get_resource(uri)
    resource.delete(headers)
  end

end
