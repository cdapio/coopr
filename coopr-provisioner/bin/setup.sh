#!/usr/bin/env bash

# Configurable variables
TIMEOUT=3
export COOPR_API_USER=${COOPR_API_USER:-admin}
export COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
export COOPR_TENANT=${COOPR_TENANT:-superadmin}
export COOPR_HOME=${COOPR_HOME:-/opt/coopr}
export COOPR_SERVER_CONF=${COOPR_HOME}/server/conf
export COOPR_RUBY=${COOPR_RUBY:-"${COOPR_HOME}/provisioner/embedded/bin/ruby"}

export COOPR_PROVISIONER_PLUGIN_DIR=${COOPR_HOME}/provisioner/worker/plugins

# $1 - Property name to read
# $2 - Config file name to read from
#
# Returns: value of a property as a string
#
# Usage: variable_to_store_value=`read_property property_name /path/to/config/file`
#
read_property () {
  case `uname -s` in
    "Darwin") sed_params="-nE" ;;
    "Linux") sed_params="-nr" ;;
  esac

  echo `grep -A 1 $1 $2 | sed $sed_params 's|.*<value>(.*)</value>.*|\1|p'`
}

# set up TLS creds

COOPR_PROTOCOL=http
COOPR_SSL=`read_property server.ssl.enabled ${COOPR_SERVER_CONF}/coopr-site.xml`
if [ "${COOPR_SSL}" == "true" ]; then
  COOPR_PROTOCOL=https
fi

export COOPR_SERVER_URI=${COOPR_SERVER_URI:-${COOPR_PROTOCOL}://localhost:55054}

export TRUST_CERT_PATH=`read_property server.ssl.trust.cert.path ${COOPR_SERVER_CONF}/coopr-security.xml`
export TRUST_CERT_PASSWORD=`read_property server.ssl.trust.cert.password ${COOPR_SERVER_CONF}/coopr-security.xml`
if [ -n TRUST_CERT_PATH ] && [ -n TRUST_CERT_PASSWORD ]; then
  export CERT_PARAMETER="--cert ${TRUST_CERT_PATH}:${TRUST_CERT_PASSWORD}"
fi

if [ "${COOPR_PROTOCOL}" == "https" ]; then
  export CURL_PARAMETER="--insecure"
fi

# TODO: steps
# - register
# - loop through upload scripts
# - sync

wait_for_plugin_registration () {
  RETRIES=0
  until [[ $(curl ${CURL_PARAMETER} --silent --request GET \
    --output /dev/null --write-out "%{http_code}" \
    --header "Coopr-UserID:${COOPR_API_USER}" \
    --header "Coopr-TenantID:${COOPR_TENANT}" \
    ${CERT_PARAMETER} \
    ${COOPR_SERVER_URI}/v2/plugins/automatortypes/chef-solo 2> /dev/null) -eq 200 || ${RETRIES} -gt 60 ]]; do
    sleep 2
    let "RETRIES++"
  done

  if [ ${RETRIES} -gt 60 ]; then
    echo "ERROR: Provisioner did not successfully register plugins"
    return 1
  fi
}

load_bundled_data ( ) {
  __skriptz=$(ls -1 ${COOPR_HOME}/provisioner/worker/plugins/*/*/load-bundled-data.sh 2>&1)
  if [ ${__skriptz} != "" ]; then
    for __i in ${__skriptz}; do
      ${__i} || return 1
    done
  else
    echo "ERROR: Cannot find any load-bundled-data.sh scripts to execute"
    return 1
  fi
  return 0
}

# Register provisioner
${COOPR_HOME}/provisioner/bin/provisioner.sh register || exit 1
wait_for_plugin_registration || exit 1

# Load plugin-bundled data
load_bundled_data || exit 1

# Request sync
curl ${CURL_PARAMETER} --silent --request POST \
  --header "Content-Type:application/json" \
  --header "Coopr-UserID:${COOPR_API_USER}" \
  --header "Coopr-ApiKey:${COOPR_API_KEY}" \
  --header "Coopr-TenantID:${COOPR_TENANT}" \
  --connect-timeout ${TIMEOUT} \
  ${CERT_PARAMETER} \
  ${COOPR_SERVER_URI}/v2/plugins/sync
__ret=${?}
[[ ${__ret} -ne 0 ]] && exit 1
exit 0
