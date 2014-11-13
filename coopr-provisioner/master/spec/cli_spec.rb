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
require File.dirname(__FILE__) + '/../lib/provisioner/cli.rb'

describe 'Provisioner::CLI' do

  it 'can parse command line arguments' do
    options = Coopr::CLI.read([
      '-c', '/tmp/config.xml',
      '-r'
    ])
    expected = {
      :configfile => '/tmp/config.xml',
      :register => true
    }
    expect(options).to eq(expected)
  end

end
