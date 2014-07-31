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

require 'rexml/document'

module Loom
  # class which reads and stores configuration settings from a property file
  class Config
    attr_reader :properties, :default_file, :descriptions

    def initialize(options)
      @properties = {} # name => value
      @descriptions = {} # name => description
      @default_file = "#{File.dirname(__FILE__)}/../../conf/provisioner-default.xml"
      @site_file = options[:configfile]
    end

    def get(val)
      val = 'provisioner.' + val.downcase unless val.downcase =~ /^provisioner\./i
      # handle boolean
      if @properties[val] =~ /^false$/i
        false
      else
        @properties[val]
      end
    end

    def load
      load_default
      load_file(@site_file) if @site_file
    end

    def load_default
      load_file
    end

    def load_file(file = @default_file)
      if File.file?(file) && File.readable?(file)
        begin

          doc = REXML::Document.new(File.open(file))
          doc.elements.each('configuration/*') do |e|
            # look for <property>..</property> elements
            next unless e.name == 'property'

            p_name = nil
            p_value = nil
            p_description = nil

            # loop through all children, looking for name/value/description
            e.elements.each do |child|
              if child.name == 'name'
                p_name = child.text
              elsif child.name == 'value'
                p_value = child.text
              elsif child.name == 'description'
                p_description = child.text
              end
            end

            # ignore anything without a name and value
            next if p_name.nil? || p_value.nil?
            # only consider 'provisioner.*' entries
            next unless p_name.downcase =~ /^provisioner\./i
            @properties[p_name.downcase] = p_value
            @descriptions[p_name.downcase] = p_description

          end
        rescue => e
          puts "Exception during parsing of config file: #{file}: #{e.message}, #{e.backtrace}"
          exit(1)
        end
      else
        puts "Could not read configuration file #{file}. Aborting."
        exit(1)
      end
    end

    def validate
      # %w(server.uri register.ip data.dir work.dir capacity )
    end

  end
end