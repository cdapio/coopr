if defined?(ChefSpec)
  ChefSpec::Runner.define_runner_method :sysctl_param

  # @example This is an example
  # expect(chef_run.to apply_sysctl_param('foo')
  #
  # @param [String] resource_name
  # the resource name
  #
  # @return [ChefSpec::Matchers::ResourceMatcher]
  #
  def apply_sysctl_param(resource_name)
    ChefSpec::Matchers::ResourceMatcher.new(:sysctl_param, :apply, resource_name)
  end

  def remove_sysctl_param(resource_name)
    ChefSpec::Matchers::ResourceMatcher.new(:sysctl_param, :remove, resource_name)
  end
end
