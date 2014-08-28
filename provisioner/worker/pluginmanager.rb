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
require 'rest_client'
require_relative 'automator'
require_relative 'provider'
require_relative 'utils'

class PluginManager
  attr_accessor :providermap, :automatormap, :tasks, :load_errors, :register_errors
  def initialize()
    @load_errors = Array.new
    @register_errors = Array.new
    @providermap = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    @automatormap = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    scan_plugins()
  end

  def load_errors?
    return !@load_errors.empty?
  end
  def register_errors?
    return !@load_errors.empty?
  end

  # scan plugins directory for json plugin definitions, load plugins 
  def scan_plugins
    # enforces directory structure from top-level: ./plugins/['providers']/[plugin-name]/*.json
    Dir["#{File.expand_path(File.dirname(__FILE__))}/plugins/*/*/*.json"].each do |jsonfile| 
      begin
        log.debug "pluginmanager scanning #{jsonfile}"
        jsondata =  JSON.parse( IO.read(jsonfile) )

        raise "missing 'name' field when loading plugin #{jsonfile}" unless jsondata.key?('name')
        p_name = jsondata['name']
        # p_description = jsondata['description'] || "No description found"
        p_providertypes = jsondata['providertypes'] || Array.new
        p_automatortypes = jsondata['automatortypes'] || Array.new

        log.debug "plugin \"#{p_name}\" configures providers: #{p_providertypes} and automators #{p_automatortypes}"

        p_providertypes.each do |providertype|
          raise "declared providertype \"#{providertype}\" is not defined" unless jsondata.key?(providertype)
          raise "declared providertype \"#{providertype}\" already defined in another plugin" if @providermap.key?(providertype)

          raise "providertype \"#{providertype}\" does not define an implementor classname" unless jsondata[providertype].key?('classname')
          # require every .rb file in the plugin top-level directory
          Dir["#{File.dirname(jsonfile)}/*.rb"].each {|file| require file }
          # check ancestor to determine plugin type
          klass = Object.const_get(jsondata[providertype]['classname'])
          if klass.ancestors.include? Object.const_get('Provider')
            raise "plugin \"#{p_name}\" attempting to load duplicate provider type \"#{providertype}\"" if @providermap.key?(providertype)
            @providermap.merge!({providertype => jsondata[providertype]})
          else
            raise "Declared provider \"#{providertype}\" implementation class \"#{jsondata[providertype]['classname']}\" must extend Provider class"
          end
        end

        p_automatortypes.each do |automatortype|
          raise "declared automatortype \"#{automatortype}\" is not defined" unless jsondata.key?(automatortype)
          raise "declared automatortype \"#{automatortype}\" already defined in another plugin" if @providermap.key?(automatortype)

          raise "automatortype \"#{automatortype}\" does not define an implentor classname" unless jsondata[automatortype].key?('classname')
          # require every .rb file in the plugin top-level directory
          Dir["#{File.dirname(jsonfile)}/*.rb"].each {|file| require file }
          # check ancestor to determine plugin type
          klass = Object.const_get(jsondata[automatortype]['classname'])
          if klass.ancestors.include? Object.const_get('Automator')
            raise "plugin \"#{p_name}\" attempting to load duplicate automator type \"#{automatortype}\"" if @automatormap.key?(automatortype)
            @automatormap.merge!({automatortype => jsondata[automatortype]})
          else
            raise "Declared automator \"#{automatortype}\" implementation class \"#{jsondata[automatortype]['classname']}\" must extend Automator class"
          end
        end
      rescue JSON::ParserError => e
        log.error "Could not load plugin, invalid json at #{jsonfile}"
        log.error e.message
        log.error e.backtrace.inspect
        @load_errors.push("Could not load plugin, invalid json at #{jsonfile}")
        next
      rescue => e
        log.error "Could not load plugin at #{jsonfile}"
        log.error e.message
        log.error e.backtrace.inspect
        @load_errors.push("Could not load plugin at #{jsonfile}")
        next
      end 
    end
  end

  def register_plugins(uri)
    @providermap.each do |name, json_obj|
      register_plugintype(name, json_obj, "#{uri}/v2/plugins/providertypes/#{name}")
    end
    @automatormap.each do |name, json_obj|
      register_plugintype(name, json_obj, "#{uri}/v2/plugins/automatortypes/#{name}")
    end
  end

  def register_plugintype(name, json_obj, uri)
    begin
      log.debug "registering provider/automator type: #{name}"
      json = JSON.generate(json_obj)
      resp = RestClient.put("#{uri}", json, :'X-Loom-UserID' => "admin", :'X-Loom-TenantID' => "superadmin")
      if(resp.code == 200)
        log.info "Successfully registered #{name}"
      else
        log.error "Response code #{resp.code}, #{resp.to_str} when trying to register #{name}"
        @register_errors.push("Response code #{resp.code}, #{resp.to_str} when trying to register #{name}")
      end
    rescue => e
      log.error "Caught exception registering plugins to loom server #{uri}"
      log.error e.message
      log.error e.backtrace.inspect
      @register_errors.push("Caught exception registering plugins to loom server #{uri}")
    end
  rescue => e
    log.error "Caught exception registering plugins to loom server #{uri}"
    log.error e.message
    log.error e.backtrace.inspect
  end

  # returns registered class name for given provider plugin
  def getHandlerActionObjectForProvider(providerName)
    if @providermap.key?(providerName)
      if @providermap[providerName].key?('classname')
        return @providermap[providerName]['classname']
      end
    end
    fail "No registered provider for #{providerName}"
  end

  # returns registered class name for given automator plugin
  def getHandlerActionObjectForAutomator(automatorName)
    if @automatormap.key?(automatorName)
      if @automatormap[automatorName].key?('classname')
        return @automatormap[automatorName]['classname']
      end
    end
    fail "No registered automator for #{automatorName}"
  end

  # returns all registered automators, used for bootstrap task
  def getAllHandlerActionObjectsForAutomators
    results = []
    @automatormap.each do |k, v|
      results.push(v['classname']) if v.key?('classname')
    end
    results
  end
end
