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

class PluginManager
  attr_accessor :providermap, :automatormap, :tasks
  def initialize()
    @providermap = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    @automatormap = Hash.new{ |h,k| h[k] = Hash.new(&h.default_proc) }
    scan_plugins()
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
        p_description = jsondata['description'] || "No description found"
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
        next
      rescue => e
        log.error "Could not load plugin at #{jsonfile}"
        log.error e.message
        log.error e.backtrace.inspect
        next
      end 
    end
  end

  def register_plugins(uri)
    @providermap.each do |name, json_obj|
      register_plugintype(name, json_obj, "#{uri}/v1/loom/providertypes/#{name}")
    end
    @automatormap.each do |name, json_obj|
      register_plugintype(name, json_obj, "#{uri}/v1/loom/automatortypes/#{name}")
    end
  end

  def register_plugintype(name, json_obj, uri)
    begin
      log.debug "registering provider/automator type: #{name}"
      json = JSON.generate(json_obj)
      resp = RestClient.put("#{uri}", json, :'X-Loom-UserID' => "admin")
      if(resp.code == 200)
        log.info "Successfully registered #{name}"
      else
        log.error "Response code #{resp.code}, #{resp.to_str} when trying to register #{name}"
      end
    rescue => e
      log.error "Caught exception registering plugins to loom server #{uri}"
      log.error e.message
      log.error e.backtrace.inspect
    end
  end

  # returns registered class name for given provider plugin
  def getHandlerActionObjectForProvider(providerName)
    if @providermap.has_key?(providerName) 
      if @providermap[providerName].has_key?('classname')
        return @providermap[providerName]['classname']
      end
    end
    raise "No registered provider for #{providerName}"
  end

  # returns registered class name for given automator plugin
  def getHandlerActionObjectForAutomator(automatorName)
    if @automatormap.has_key?(automatorName) 
      if @automatormap[automatorName].has_key?('classname')
        return @automatormap[automatorName]['classname']
      end
    end
    raise "No registered automator for #{automatorName}"
  end

  # returns all registered automators, used for bootstrap task
  def getAllHandlerActionObjectsForAutomators
    results = Array.new
    @automatormap.each do |k, v|
      if (v.has_key?('classname')) 
        results.push(v['classname'])
      end
    end
    results
  end

end
    

