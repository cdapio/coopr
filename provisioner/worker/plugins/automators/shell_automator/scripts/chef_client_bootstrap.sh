#!/bin/bash
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

# This script assumes that chef-client is installed and available on the client.
#
# Arguments are: chef_server_url [run_list]

SCRIPT_DIR="/var/cache/loom/scripts"
CHEF_DIR="/etc/chef"

CHEF_SERVER_URL=${1}
shift
RUN_LIST=$*

if [ "${CHEF_SERVER_URL}" != "" ] ; then
  echo "Chef Server: ${CHEF_SERVER_URL}"
else
  echo "No CHEF_SERVER_URL set"
  exit 1
fi

if [ "${RUN_LIST}" != "" ] ; then
  RUN_LIST_JSON="{\"run_list\":[\"${RUN_LIST}\"]}"
else
  RUN_LIST_JSON="{\"run_list\":[]}"
fi

mkdir -p /etc/chef

for f in validation.pem encrypted_data_bag_secret ; do
  if [ -e ${SCRIPT_DIR}/${f} ] ; then
    cp -f ${SCRIPT_DIR}/${f} ${CHEF_DIR}
    chmod 0600 ${CHEF_DIR}/${f}
  fi
done

echo ${RUN_LIST_JSON} > ${CHEF_DIR}/first-boot.json

cat > /etc/chef/client.rb <<-CONFIG
log_level        :auto
log_location     STDOUT
chef_server_url  "${CHEF_SERVER_URL}"
validation_client_name "chef-validator"
CONFIG

chef-client -j /etc/chef/first-boot.json
