require 'chef/provider'

class Chef
  class Provider
    class DpkgAutostart < Chef::Provider
      def initialize(*args)
        super
      end

      def load_current_resource
      end

      def action_run
        Chef::Log.debug 'DpkgAutostart: Compat method. Action does nothing.'
      end

      def action_create
        template = Chef::Resource::Template.new('dpkg_autostart_file', run_context)
        template.cookbook 'dpkg_autostart'
        template.source 'policy-rc.d.erb'
        template.path '/usr/sbin/policy-rc.d'
        template.mode 0755
        template.only_if{ node.platform_family?('debian') }
        template.run_action(:create)
        # Force this to the front of the line
      end
    end
  end
end
