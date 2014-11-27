include_recipe 'polipo'

# Ensure the containers can hit the proxy
node.default[:polipo][:config][:allowed_clients] = node[:polipo][:config][:allowed_clients].
  split(',').map(&:strip).push(node[:lxc][:default_config][:lxc_network]).join(', ')

iptables_ng_rule 'lxc-proxy-http' do
  rule lazy{
    "-A OUTPUT -p tcp --source #{node[:lxc][:default_config][:lxc_network]} --dport 80 -m owner ! --uid-owner #{node[:polipo][:service][:user]} -j ACCEPT"
  }
end
