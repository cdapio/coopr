use_inline_resources if self.respond_to?(:use_inline_resources)

def load_current_resource
  @lxc = ::Lxc.new(
    new_resource.base_container,
    :base_dir => node[:lxc][:container_directory],
    :dnsmasq_lease_file => node[:lxc][:dnsmasq_lease_file]
  )
  unless(@lxc.exists?)
    raise "Requested base contianer: #{new_resource.base_container} does not exist"
  end
end

action :run do

  require 'elecksee/ephemeral'

  ephemeral_args = {
    :original => new_resource.base_container,
    :bind => new_resource.bind_directory,
    :union => new_resource.union_type
  }
  {
    :daemon => :background,
    :directory => :host_rootfs,
    :virtual_device => :virtual_device,
    :ephemeral_command => :command
  }.each do |k,v|
    ephemeral_args[k] = new_resource.send(v) if new_resource.send(v)
  end

  ::Lxc::Ephemeral.new(ephemeral_args).start!

  # If we ran, we were updated
  new_resource.updated_by_last_action(true)
end
