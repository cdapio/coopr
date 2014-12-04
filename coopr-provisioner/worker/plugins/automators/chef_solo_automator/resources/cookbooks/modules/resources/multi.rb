actions :save, :remove

default_action :save

attribute :name, :kind_of => String, :name_attribute => true
attribute :path, :kind_of => String, :default => nil
attribute :modules, :kind_of => Array, :default => nil, :required => true

# Covers 0.10.8 and earlier
def initialize(*args)
  super
  @action = :save
end
