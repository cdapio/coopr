# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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
require File.dirname(__FILE__) + '/../lib/provisioner/config.rb'

describe 'Loom::Config' do

  before do
    @config = Loom::Config.new({})
  end

  it 'can parse the included config file' do
    @config.load_file("#{File.dirname(__FILE__)}/loom-site.xml")
    expect(@config.get_value('provisioner.bind.ip')).to eq('0.0.0.0')
    expect(@config.get_value('bind.ip')).to eq('0.0.0.0')
  end

end