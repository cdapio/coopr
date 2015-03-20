#!/usr/bin/env bash
#
# Copyright © 2012-2015 Cask Data, Inc.
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

die ( ) { echo; echo "ERROR: ${*}"; echo; exit 1; }

COOPR_NODE=${COOPR_NODE:-node}
COOPR_NPM=${COOPR_NPM:-npm}
COOPR_GULP='./node_modules/gulp/bin/gulp.js'

${COOPR_NPM} run prebuild || die "npm failed"
${COOPR_NODE} ${COOPR_GULP} distribute || die "gulp failed"
