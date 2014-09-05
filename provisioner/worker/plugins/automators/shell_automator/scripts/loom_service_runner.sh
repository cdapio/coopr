#!/usr/bin/env bash
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

usage() {
  echo "Usage: ${0} service action"
  echo "       service: service to manipulate"
  echo "       action:  action to be performed on service"
  echo
  exit 1
}

get_service_bin() {
  [[ ${LOOM_SERVICE_BIN} ]] && return
  if [[ -x /sbin/service ]] ; then
    LOOM_SERVICE_BIN="/sbin/service "
  elif [[ -x /usr/sbin/service ]] ; then
    LOOM_SERVICE_BIN="/usr/sbin/service "
  else
    LOOM_SERVICE_BIN="/etc/init.d/"
  fi
}

# Input is: servicename action
if [[ ${#@} -ne 2 ]] ; then
  usage
else
  SERVICE_NAME=${1}
  ACTION=${2}
fi

get_service_bin

${LOOM_SERVICE_BIN}${SERVICE_NAME} ${ACTION}
