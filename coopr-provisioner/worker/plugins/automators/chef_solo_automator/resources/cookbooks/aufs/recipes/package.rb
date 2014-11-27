case node['platform']
when 'ubuntu'
  # If aufs isn't available, do our best to install the correct
  # linux-image-extra package.
  # Use kernel release for saucy and newer, otherwise use older,
  # more compatible regexp match
  if Chef::Version.new(node['platform_version']) < Chef::Version.new('13.10')
    # Original method copied from:
    # https://github.com/thoward/docker-cookbook/blob/master/recipes/default.rb
    uname = 'uname -r | grep --only-matching -e [0-9]\.[0-9]\.[0-9]-[0-9]*'
    extra_package = 'linux-image-extra-' + Mixlib::ShellOut.new(
      uname).run_command.stdout.strip
  else
    # In modern ubuntu versions, kernel release matches the kernel package name
    extra_package = 'linux-image-extra-' + node['kernel']['release']
  end

  # Ensure AUFS package is available
  aufs_package = Mixlib::ShellOut.new(
    'apt-cache search ' + extra_package).run_command.stdout.split(' ').first

  if aufs_package && !aufs_package.empty?
    # Wait to strip for protection against nil errors
    package aufs_package.strip do
      not_if 'modprobe -n -v aufs'
    end
  end
end
