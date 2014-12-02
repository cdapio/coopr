def load_current_resource
  @lxc = ::Lxc.new(
    new_resource.name,
    :base_dir => node[:lxc][:container_directory],
    :dnsmasq_lease_file => node[:lxc][:dnsmasq_lease_file]
  )

  if(node[:lxc][:bugfix][:precise][:repo][:enabled] &&
      node[:lxc][:bugfix][:precise][:repo][:auto_enable_lwrp] &&
      new_resource.template == 'ubuntu' && new_resource.template_opts.fetch('--release', 'precise') == 'precise')
    new_resource.environment.merge(
      'LOCAL_REPO' => "file://#{File.join(node[:lxc][:bugfix][:precise][:repo][:path], 'precise')}"
    )
  end
end

action :create do
  _lxc = @lxc
  execute "LXC Create: #{new_resource.name}" do
    command "lxc-create -n #{new_resource.name} -t #{new_resource.template} -- #{new_resource.template_opts.to_a.flatten.join(' ')}"
    environment new_resource.environment
    only_if do
      !_lxc.exists? && new_resource.updated_by_last_action(true)
    end
  end
end

action :clone do

  require 'elecksee/clone'

  _lxc = @lxc

  unless(::Lxc.new(new_resource.base_container).exists?)
    raise "LXC clone failed! Base container #{new_resource.base_container} does not exist. Cannot create #{new_resource.name}"
  end

  ruby_block "LXC Clone: #{new_resource.base_container} -> #{new_resource.name}" do
    block do
      cloner = ::Lxc::Clone.new(
        :original => new_resource.base_container,
        :new_name => new_resource.name
      )
      cloner.clone!
    end
    only_if do
      !_lxc.exists? && new_resource.updated_by_last_action(true)
    end
  end
end

action :delete do
  _lxc = @lxc
  ruby_block "Stop container #{new_resource.name}" do
    block do
      _lxc.shutdown
    end
    only_if do
      _lxc.exists? && _lxc.running?
    end
  end

  execute "Destroy container #{new_resource.name}" do
    command "lxc-destroy -n #{new_resource.name}"
    only_if do
      _lxc.exists?
    end
  end
end
