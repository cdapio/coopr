#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'json'
require 'optparse'
require 'rest_client'
require 'socket'
require 'logger'
require 'fileutils'

require_relative 'utils.rb'
require_relative 'pluginmanager.rb'
require_relative 'provider.rb'
require_relative 'automator.rb'
require_relative '../bin/rest-helper'

$stdout.sync = true

# Parse command line options.
options = {}
OptionParser.new do |opts|
  opts.banner = 'Usage: '
  opts.on('-u', '--uri URI', 'Coopr web server uri') do |u|
    options[:uri] = u
  end
  opts.on('-f', '--file FILE', 'Full path to task json') do |f|
    options[:file] = f
  end
  opts.on('-t', '--tenant TENANT', 'Tenant ID') do |t|
    options[:tenant] = t
  end
  opts.on('-p', '--provisioner PROVISIONER', 'Provisioner ID') do |p|
    options[:provisioner] = p
  end
  opts.on('-n', '--name NAME', 'Worker name') do |n|
    options[:name] = n
  end
  options[:register] = false
  opts.on('-r', '--register', 'Register installed plugins with the server.  Requires --uri') do
    options[:register] = true
  end
  opts.on('-L', '--log-level LEVEL', 'Log level') do |f|
    options[:log_level] = f
  end
  opts.on('-l', '--log-dir DIRECTORY', 'Path to log directory') do |d|
    options[:log_directory] = d
  end
  opts.on('-w', '--work-dir DIRECTORY', 'Path to work directory') do |d|
    options[:work_dir] = d
  end
  options[:once] = false
  opts.on('-o', '--once', 'Only poll and run a single task') do
    options[:once] = true
  end
end.parse!

coopr_uri = options[:uri]
if coopr_uri.nil? && !options[:file]
  puts 'Either URI for coopr server or --file must be specified'
  exit(1)
end

unless options[:work_dir]
  puts '--work-dir option must be specified'
  exit(1)
end

unless options[:tenant] || options[:register]
  puts 'Either --tenant or --register options must be specified'
  exit(1)
end

if coopr_uri.nil? && options[:register]
  puts '--register option requires the --uri [server uri] option'
  exit(1)
end

include Logging
log_file = nil
if options[:log_directory] && options[:name]
  log_file = [options[:log_directory], options[:name]].join('/') + '.log'
elsif options[:log_directory]
  log_file = "#{options[:log_directory]}/worker-default.log"
end
Logging.configure(log_file)
Logging.level = options[:log_level]
Logging.process_name = options[:name] if options[:name]

# load plugins
pluginmanager = PluginManager.new

# ensure we have at least one plugin of each type for task coverage
if pluginmanager.providermap.empty? or pluginmanager.automatormap.empty?
  log.fatal 'Error: at least one provider plugin and one automator plugin must be installed'
  exit(1)
end

# the environment passed to plugins
@plugin_env = options

def _run_plugin(clazz, env, cwd, task)
  object = clazz.new(env, task)
  FileUtils.mkdir_p(cwd)
  Dir.chdir(cwd) do
    result = object.runTask
  end
end

def delegate_task(task, pluginmanager)
  providerName = nil # rubocop:disable UselessAssignment
  automatorName = nil # rubocop:disable UselessAssignment
  clazz = nil # rubocop:disable UselessAssignment
  object = nil
  result = nil
  classes = nil
  task_id = task['taskId']

  log.debug "Processing task with id #{task_id} ..."

  taskName = task['taskName'].downcase
  # depending on task, these may be nil
  # automator take pecedence as presence indicates a 'software' task
  providerName = task['config']['provider']['providertype'] rescue nil
  automatorName = task['config']['service']['action']['type'] rescue nil

  case taskName.downcase
  when 'create', 'confirm', 'delete'
    clazz = Object.const_get(pluginmanager.getHandlerActionObjectForProvider(providerName))
    cwd = File.join(@plugin_env[:work_dir], @plugin_env[:tenant], 'providertypes', providerName)
    result = _run_plugin(clazz, @plugin_env, cwd, task)
  when 'install', 'configure', 'initialize', 'start', 'stop', 'remove'
    clazz = Object.const_get(pluginmanager.getHandlerActionObjectForAutomator(automatorName))
    cwd = File.join(@plugin_env[:work_dir], @plugin_env[:tenant], 'automatortypes', automatorName)
    result = _run_plugin(clazz, @plugin_env, cwd, task)
  when 'bootstrap'
    combinedresult = {}
    classes = []
    if task['config'].key? 'automators' and !task['config']['automators'].empty?
      # server must specify which bootstrap handlers need to run
      log.debug "Task #{task_id} running specified bootstrap handlers: #{task['config']['automators']}"
      task['config']['automators'].each do |automator|
        clazz = Object.const_get(pluginmanager.getHandlerActionObjectForAutomator(automator))
        cwd = File.join(@plugin_env[:work_dir], @plugin_env[:tenant], 'automatortypes', automator)
        result = _run_plugin(clazz, @plugin_env, cwd, task)
        combinedresult.merge!(result)
      end
    else
      log.warn 'No automators specified to bootstrap'
    end
    result = combinedresult
  else
    log.error "Unhandled task of type #{task['taskName']}"
    fail "Unhandled task of type #{task['taskName']}"
  end
  result
end

# register plugins with the server if --register flag passed
if options[:register]
  pluginmanager.register_plugins(coopr_uri)
  if pluginmanager.load_errors?
    log.error 'There was at least one provisioner plugin load failure'
    exit(1)
  end
  if pluginmanager.register_errors?
    log.error 'There was at least one provisioner plugin register failure'
    exit(1)
  end
  exit(0)
end

log.debug "provisioner starting with provider types: #{pluginmanager.providermap.keys}"
log.debug "provisioner starting with automator types: #{pluginmanager.automatormap.keys}"

if options[:file]
  # run a single task read from file
  begin
    result = nil
    task = nil
    log.info "Start Provisioner run for file #{options[:file]}"
    task = JSON.parse(IO.read(options[:file]))

    # While provisioning, don't allow the provisioner to terminate by disabling signal
    sigterm = SignalHandler.new('TERM')
    sigterm.dont_interupt {
      result = delegate_task(task, pluginmanager)
    }
  rescue => e
    log.error "Caught exception when running task from file #{options[:file]}"

    result = {} if result.nil? == true
    result['status'] = '1'
    if e.class.name == 'CommandExecutionError'
      log.error "#{e.class.name}: #{e.to_json}"
      result['stdout'] = e.stdout
      result['stderr'] = e.stderr
    else
      result['stdout'] = e.inspect
      result['stderr'] = "#{e.inspect}\n#{e.backtrace.join("\n")}"
    end
    log.error "Provisioner run failed, result: #{result}"
  end
else
  # run in server polling mode

  pid = Process.pid
  host = Socket.gethostname.downcase
  myid = "#{host}.#{pid}"

  log.info "Starting provisioner with id #{myid}, connecting to server #{coopr_uri}"

  loop {
    result = nil
    response = nil
    task = nil
    begin
      response = RestHelper.post "#{coopr_uri}/v2/tasks/take", { 'provisionerId' => options[:provisioner], 'workerId' => myid, 'tenantId' => options[:tenant] }.to_json
    rescue => e
      log.error "Caught exception connecting to coopr server #{coopr_uri}/v2/tasks/take: #{e}"
      sleep 10
      next
    end

    begin
      if response.code == 200 && response.to_str && response.to_str != ''
        task = JSON.parse(response.to_str)
        log.debug "Received task from server <#{response.to_str}>"
      elsif response.code == 204
        break if options[:once]
        sleep 1
        next
      else
        log.error "Received error code #{response.code} from coopr server: #{response.to_str}"
        sleep 10
        next
      end
    rescue => e
      log.error "Caught exception processing response from coopr server: #{e.inspect}"
    end

    # While provisioning, don't allow the provisioner to terminate by disabling signal
    sigterm = SignalHandler.new('TERM')
    sigterm.dont_interupt {
      begin
        result = delegate_task(task, pluginmanager)

        result = Hash.new if result.nil? == true
        result['workerId'] = myid
        result['taskId'] = task['taskId']
        result['provisionerId'] = options[:provisioner]
        result['tenantId'] = options[:tenant]

        log.debug "Task <#{task['taskId']}> completed, updating results <#{result}>"
        begin
          response = RestHelper.post "#{coopr_uri}/v2/tasks/finish", result.to_json
        rescue => e
          log.error "Caught exception posting back to coopr server #{coopr_uri}/v2/tasks/finish: #{e}"
        end

      rescue => e
        result = Hash.new if result.nil? == true
        result['status'] = '1'
        result['workerId'] = myid
        result['taskId'] = task['taskId']
        result['provisionerId'] = options[:provisioner]
        result['tenantId'] = options[:tenant]
        if e.class.name == 'CommandExecutionError'
          log.error "#{e.class.name}: #{e.to_json}"
          result['stdout'] = e.stdout
          result['stderr'] = e.stderr
        else
          result['stdout'] = e.inspect
          result['stderr'] = "#{e.inspect}\n#{e.backtrace.join("\n")}"
        end
        log.error "Task <#{task['taskId']}> failed, updating results <#{result}>"
        begin
          response = RestHelper.post "#{coopr_uri}/v2/tasks/finish", result.to_json
        rescue => e
          log.error "Caught exception posting back to server #{coopr_uri}/v2/tasks/finish: #{e}"
        end
      end
    }

    break if options[:once]
    sleep 5
  }

end
