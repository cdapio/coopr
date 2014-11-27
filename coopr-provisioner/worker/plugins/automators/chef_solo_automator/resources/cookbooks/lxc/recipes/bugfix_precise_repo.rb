file node[:lxc][:bugfix][:precise][:repo][:exec] do
  path 'lxc-precise-lts'
  mode 0755
  action node[:lxc][:bugfix][:precise][:repo][:enabled] ? :create : :delete
  only_if{ node[:lxc][:bugfix][:precise][:repo][:enabled] }
end

confirm_file = File.join(node[:lxc][:bugfix][:precise][:repo][:path], 'confirm')

file confirm_file do
  action :nothing
  content Time.now.to_s
end

execute 'LXC Precise LTS bugfix repository create' do
  command node[:lxc][:bugfix][:precise][:repo][:exec]
  environment 'REPO_PATH' => node[:lxc][:bugfix][:precise][:repo][:path]
  notifies :create, "file[#{confirm_file}]", :immediately
  only_if do
    node[:lxc][:bugfix][:precise][:repo][:enabled] &&
      !File.exists?(confirm_file)
  end
end
