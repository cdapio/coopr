
package 'polipo' do
  if(ver = node[:polipo][:install][:version])
    version ver
  end
  if(act = node[:polipo][:install][:action])
    action act.to_sym
  end
  notifies :restart, 'service[polipo]'
end

file File.join(node[:polipo][:directories][:config], 'config') do
  content lazy{
    node[:polipo][:config].map do |key, value|
      parts = key.split('_')
      "#{parts.first}#{parts[1,parts.size].map(&:capitalize).join} = #{value}"
    end.join("\n")
  }
  notifies :restart, 'service[polipo]'
end

file File.join(node[:polipo][:directories][:config], 'options') do
  content lazy{
    node[:polipo][:options][:method].map do |value|
      "method #{value}"
    end.join("\n")
  }
  notifies :restart, 'service[polipo]'
end

file File.join(node[:polipo][:directories][:config], 'forbidden') do
  content lazy{ node[:polipo][:forbidden].join("\n") }
  notifies :restart, 'service[polipo]'
end

service 'polipo' do
  action :start
end
