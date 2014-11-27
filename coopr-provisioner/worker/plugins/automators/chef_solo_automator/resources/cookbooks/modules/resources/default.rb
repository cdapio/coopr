actions :save, :load, :remove

default_action :save

attribute :module, :kind_of => String, :name_attribute => true
attribute :options, :kind_of => Hash, :default => nil
attribute :path, :kind_of => String, :default => nil

# Covers 0.10.8 and earlier
def initialize(*args)
  super
  @action = :save
end
