require File.expand_path '../spec_helper.rb', __FILE__
require File.dirname(__FILE__) + '/../lib/provisioner/cli.rb'

describe 'Provisioner::CLI' do

  it 'can parse command line arguments' do
    options = Loom::CLI.read([
        '-u', 'http://test',
        '-t', 'test_tenant',
        '-p', 'test_provisioner',
        '-L', 'debug',
        '-l', '/tmp/test.log',
        '-b'
                             ])
    expected = {
        :uri => 'http://test',
        :tenant => 'test_tenant',
        :provisioner => 'test_provisioner',
        :log_level => 'debug',
        :log_file => '/tmp/test.log',
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
