require File.expand_path('../support/helpers', __FILE__)

describe_recipe 'aufs::default' do
  include Helpers::Aufs
end
