#
# Cookbook Name:: iptables-ng
# Library:: mattchers
#
# Copyright 2014, Dan Fruehauf
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

# Matchers for chefspec
if defined?(ChefSpec)
  def create_iptables_ng_rule(resource_name)
    ChefSpec::Matchers::ResourceMatcher.new(:iptables_ng_rule, :create, resource_name)
  end

  def create_iptables_ng_chain(resource_name)
    ChefSpec::Matchers::ResourceMatcher.new(:iptables_ng_chain, :create, resource_name)
  end

  def create_if_missing_iptables_ng_chain(resource_name)
    ChefSpec::Matchers::ResourceMatcher.new(:iptables_ng_chain, :create_if_missing, resource_name)
  end
end
