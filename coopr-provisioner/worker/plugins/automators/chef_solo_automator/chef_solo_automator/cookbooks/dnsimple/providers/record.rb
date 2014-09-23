#
# Copyright:: Copyright (c) 2010-2011, Heavy Water Software
# Copyright:: Copyright (c) 2012, Joshua Timberman
# License:: Apache License, Version 2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include DNSimple::Connection

def whyrun_supported?
  true
end

def load_current_resource
  @current_resource = Chef::Resource::DnsimpleRecord.new(@new_resource.name)
  @current_resource.name(@new_resource.name)
  @current_resource.domain(@new_resource.domain)

  @current_resource.exists = check_for_record
end

action :create do
  case @current_resource.exists
  when :same
    Chef::Log.info "#{ @new_resource } already exists - nothing to do."
  when :different
    converge_by("Updating #{ @new_resource }") do
      delete_record
      create_record
    end
  when :none
    converge_by("Creating #{ @new_resource }") do
      create_record
    end
  end
end

action :destroy do
  delete_record
end

def check_for_record
  all_records_in_zone do |r|
    if ((r.name == new_resource.name) && (r.type == new_resource.type))
      if ((r.value == new_resource.content) && (r.ttl == new_resource.ttl))
        return :same
      else
        return :different
      end
    end
  end
  return :none
end

def create_record
  Chef::Log.debug "Attempting to create record type #{new_resource.type} for #{new_resource.name} as #{new_resource.content} with type #{new_resource.type}"
  zone = dnsimple.zones.get( new_resource.domain )
  record = zone.records.create( :name  => new_resource.name,
                               :value => new_resource.content,
                               :type  => new_resource.type,
                               :ttl   => new_resource.ttl )
  new_resource.updated_by_last_action(true)
  Chef::Log.info "DNSimple: created #{new_resource.type} record for #{new_resource.name}.#{new_resource.domain}"
rescue Excon::Errors::UnprocessableEntity
  Chef::Log.debug "DNSimple: #{new_resource.name}.#{new_resource.domain} already exists, moving on"
end

def delete_record
  if [:same, :different].include?(@current_resource.exists)
    all_records_in_zone do |r|
      if (( r.name == new_resource.name ) && ( r.type == new_resource.type ))
        r.destroy
        new_resource.updated_by_last_action(true)
        Chef::Log.info "DNSimple: destroyed #{new_resource.type} record " +
          "for #{new_resource.name}.#{new_resource.domain}"
      end
    end
  else
    Chef::Log.info "DNSimple: no record found #{new_resource.name}.#{new_resource.domain}" +
      " with type #{new_resource.type}"
  end
end

def all_records_in_zone
  zone = dnsimple.zones.get( new_resource.domain )

  zone.records.all.each do |r|
     yield r if block_given?
  end
end