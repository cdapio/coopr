def supported?
  case node['platform']
  when "ubuntu"
    # only work with upstart
    if Gem::Version.new(node['platform_version']) >= Gem::Version.new("9.10")
      return true
    else
      return false
    end
  else
    Chef::Log.info("Your platform isn't manage to save module changes")
    return false
  end
end
