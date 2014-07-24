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


# simple specification for a tenant
module Loom
  class TenantSpec
    attr_accessor :id, :workers, :modules, :plugins

    def initialize(id, workers, modules = nil, plugins = nil)
      @id = id
      @workers = workers
      # @modules = modules ||= Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
      # @plugins = plugins ||= Hash.new { |h, k| h[k] = Hash.new(&h.default_proc) }
    end
  end
end
