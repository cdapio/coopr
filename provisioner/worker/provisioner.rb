#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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

require_relative 'utils.rb'
require_relative 'pluginmanager.rb'
require_relative 'provider.rb'
require_relative 'automator.rb'

$stdout.sync = true

# Parse command line options.
options = {}
OptionParser.new do |opts|
  opts.banner = 'Usage: '
  opts.on('-u', '--uri URI', 'Loom web server uri') do |u|
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
    options[:work_directory] = d
  end
  options[:once] = false
  opts.on('-o', '--once', 'Only poll and run a single task') do
    options[:once] = true
  end
end.parse!

loom_uri = options[:uri]
if loom_uri.nil? && !options[:file]
  puts 'Either URI for loom server or --file must be specified'
  exit(1)
end

if(loom_uri.nil? && options[:register])
  puts "--register option requires the --uri [server uri] option"
  exit(1)
end

include Logging
log_file = nil
if options[:log_directory] && options[:name]
  log_file = [ options[:log_directory], options[:name] ].join('/') + '.log'
elsif options[:log_directory]
  log_file = "#{options[:log_directory]}/worker-default.log"
end
Logging.configure(log_file)
Logging.level = options[:log_level]

# load plugins
pluginmanager = PluginManager.new

# ensure we have at least one plugin of each type for task coverage
if pluginmanager.providermap.empty? or pluginmanager.automatormap.empty?
  log.fatal 'Error: at least one provider plugin and one automator plugin must be installed'
  exit(1)
end

def delegate_task(task, pluginmanager)
  task_id = nil
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
    object = clazz.new(task)
    result = object.runTask
  when 'install', 'configure', 'initialize', 'start', 'stop', 'remove'
    clazz = Object.const_get(pluginmanager.getHandlerActionObjectForAutomator(automatorName))
    object = clazz.new(task)
    result = object.runTask
  when 'bootstrap'
    combinedresult = {}
    classes = []
    if task['config'].key? 'automators' and !task['config']['automators'].empty?
      # server has specified which bootstrap handlers need to run
      log.debug "Task #{task_id} running specified bootstrap handlers: #{task['config']['automators']}"
      task['config']['automators'].each do |automator|
        classes.push(pluginmanager.getHandlerActionObjectForAutomator(automator))
      end
    else
      # default to running all registered bootstrap handlers
      classes = pluginmanager.getAllHandlerActionObjectsForAutomators
      log.debug "Task #{task_id} running bootstrap handlers: #{classes}"
    end
    fail 'No bootstrappers configured' if classes.empty?

    classes.each do |klass|
      klass = Object.const_get(klass)
      object = klass.new(task)
      result = object.runTask
      combinedresult.merge!(result)
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
  pluginmanager.register_plugins(loom_uri)
  if (pluginmanager.load_errors?)
    log.error "There was at least one provisioner plugin load failure"
    exit(1)
  end
  if (pluginmanager.register_errors?)
    log.error "There was at least one provisioner plugin register failure"
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
  rescue Exception => e
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

  log.info "Starting provisioner with id #{myid}, connecting to server #{loom_uri}"

  loop {
    result = nil
    response = nil
    task = nil
    begin
      response = RestClient.post "#{loom_uri}/v1/loom/tasks/take", { 'provisionerId' => options[:provisioner], 'workerId' => myid, 'tenantId' => options[:tenant] }.to_json
    rescue => e
      log.error "Caught exception connecting to loom server #{loom_uri}/v1/loom/tasks/take: #{e}"
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
        log.error "Received error code #{response.code} from loom server: #{response.to_str}"
        sleep 10
        next
      end
    rescue => e
      log.error "Caught exception processing response from loom server: #{e.inspect}"
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

        log.debug "Task <#{task["taskId"]}> completed, updating results <#{result}>"
        begin
          response = RestClient.post "#{loom_uri}/v1/loom/tasks/finish", result.to_json
        rescue => e
          log.error "Caught exception posting back to loom server #{loom_uri}/v1/loom/tasks/finish: #{e}"
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
        log.error "Task <#{task["taskId"]}> failed, updating results <#{result}>"
        begin
          response = RestClient.post "#{loom_uri}/v1/loom/tasks/finish", result.to_json
        rescue => e
          log.error "Caught exception posting back to server #{loom_uri}/v1/loom/tasks/finish: #{e}"
        end
      end
    }

    break if options[:once]
    sleep 5
  }

end
