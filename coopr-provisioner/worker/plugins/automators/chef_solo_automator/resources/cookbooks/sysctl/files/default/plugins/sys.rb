#
# Author:: Sander van Zoest (<svanzoest@onehealth.com>)
# Copyright:: Copyright (c) 2014 OneHealth Solutions, Inc.
# License:: Apache License, Version 2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.

Ohai.plugin(:Sysctl) do
  provides 'sys'

  def get_sysctls(cmd = 'sysctl -A')
    begin
      require 'chef/mixin/deep_merge'
    rescue LoadError => e
      Ohai::Log.warn("Cannot load gem: #{e}.")
    end

    Ohai::Log.debug("get_sysctl: running #{cmd}")
    so = shell_out(cmd)
    lines_attrs = Mash.new
    if so.exitstatus == 0
      so.stdout.lines do |line|
        k, v = line.split(/[=:]/)
        next if k.nil? || v.nil?
        k = k.strip
        v = v.strip
        key_path = k.split('.')
        attrs = Mash.new
        location = key_path.slice(0, key_path.size - 1).reduce(attrs) do |m, o|
          m[o] ||= {}
          m[o]
        end
        location[key_path.last] = v
        lines_attrs = Chef::Mixin::DeepMerge.merge(lines_attrs, attrs)
      end
    end
    sys.update lines_attrs
  end

  collect_data(:default) do
    sys Mash.new
  end

# :aix, :darwin, :freebsd, :hpux, :linux, :openbsd, :netbsd, :solaris2, :windows
#  or any other value from RbConfig::CONFIG['host_os']

  collect_data(:linux) do
    sys Mash.new
    get_sysctls
  end

  collect_data(:darwin) do
    sys Mash.new
    get_sysctls
  end
end
