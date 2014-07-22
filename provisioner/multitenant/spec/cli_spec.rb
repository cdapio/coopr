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
require File.dirname(__FILE__) + '/../lib/provisioner/cli.rb'

describe 'Provisioner::CLI' do

  it 'can parse command line arguments' do
    options = Loom::CLI.read([
        '-u', 'http://test',
        '-L', 'debug',
        '-l', '/tmp/test.log',
        '-b', '127.0.0.1',
        '-p', '55058',
        '-d'
                             ])
    expected = {
        :uri => 'http://test',
        :log_level => 'debug',
        :log_directory => '/tmp/test.log',
        :bind_ip => '127.0.0.1',
        :bind_port => '55058',
        :daemonize => true
    }
    expect(options).to eq(expected)
  end

  it 'needs either file or uri arguments' do
    expect { Loom::CLI.read(['-L' 'debug']) }.to raise_error SystemExit
  end

  it 'cannot register without an uri argument' do
    expect { Loom::CLI.read(['-r']) }.to raise_error SystemExit
  end

end
