require 'chef/resource'

class Chef
  class Resource
    class DpkgAutostart < Chef::Resource

      def initialize(name, run_context=nil)
        super
        @resource_name = :dpkg_autostart
        @provider = Chef::Provider::DpkgAutostart
        @action = :nothing
        @allowed_actions = [:nothing, :create]
      end

      def allow(val)
        node.run_state[:dpkg_autostart_disabled] ||= []
        node.run_state[:dpkg_autostart_disabled].push(name) unless val
        chef_version = Gem::Version.new(Chef::VERSION)
        context = chef_version < Gem::Version.new('11.0.0') ? run_context : node.run_context
        begin
          context.resource_collection.lookup('dpkg_autostart[bin_file]')
          true
        rescue Chef::Exceptions::ResourceNotFound
          bin = Chef::Resource::DpkgAutostart.new('bin_file', context)
          bin.action :create
          current_resources = context.resource_collection.all_resources
          [bin, current_resources].flatten.each_with_index do |res, i|
            context.resource_collection[i] = res
          end
        end
      end
    end
  end
end
