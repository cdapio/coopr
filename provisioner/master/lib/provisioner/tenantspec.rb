#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright © 2012-2014, Continuuity, Inc.
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

require_relative 'resourcespec'

# simple specification for a tenant
module Loom
  class TenantSpec
    attr_accessor :id, :workers, :resourcespec, :plugins

    def initialize(id, workers, resources = nil, plugins = nil)
      @id = id
      @workers = workers
      @resourcespec = ResourceSpec.new(resources)
      # @plugins = plugins ||= Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
    end

    # define two TenantSpecs as equal if @workers and @resourcespec are equal
    def ==(other)
      @workers == other.workers && @resourcespec == other.resourcespec
    end

  end
end
