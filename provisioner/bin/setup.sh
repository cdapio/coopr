#!/usr/bin/env bash

# Configurable variables
TIMEOUT=3
export LOOM_SERVER_URI=${LOOM_SERVER_URI:-http://localhost:55054}
export LOOM_API_USER=${LOOM_API_USER:-admin}
export LOOM_API_KEY=${LOOM_API_KEY:-1234567890abcdef}
export LOOM_TENANT=${LOOM_TENANT:-superadmin}
export LOOM_HOME=${LOOM_HOME:-/opt/loom}
export LOOM_RUBY=${LOOM_RUBY:-"${LOOM_HOME}/provisioner/embedded/bin/ruby"}

export LOOM_PROVISIONER_PLUGIN_DIR=${LOOM_HOME}/provisioner/worker/plugins

# TODO: steps
# - register
# - loop through upload scripts
# - sync

function wait_for_plugin_registration () {
  RETRIES=0
  until [[ $(curl --silent --request GET \
    --output /dev/null --write-out "%{http_code}" \
    --header "X-Loom-UserID:${LOOM_API_USER}" \
    --header "X-Loom-TenantID:${LOOM_TENANT}" \
    ${LOOM_SERVER_URI}/v2/plugins/automatortypes/chef-solo 2> /dev/null) -eq 200 || $RETRIES -gt 60 ]]; do
    sleep 2
    ((RETRIES++))
  done

  if [ $RETRIES -gt 60 ]; then
    echo "ERROR: Provisioner did not successfully register plugins"
    return 1
  fi
}

# Register provisioner
${LOOM_HOME}/provisioner/bin/provisioner.sh register
__ret=$?
[[ ${__ret} -ne 0 ]] && exit 1

wait_for_plugin_registration || exit 1

# Load plugin-bundled data
__skriptz=$(ls -1 ${LOOM_HOME}/provisioner/worker/plugins/*/*/load-bundled-data.sh 2>&1)
if [ "${__skriptz}" != "" ] ; then
  for __i in ${__skriptz} ; do
    ${__i}
    __ret=$?
    [[ ${__ret} -ne 0 ]] && exit 1
  done
else
  echo "Cannot find anything to execute"
  exit 1
fi

# Request sync
curl --silent --request POST \
  --header "Content-Type:application/json" \
  --header "X-Loom-UserID:${LOOM_API_USER}" \
  --header "X-Loom-ApiKey:${LOOM_API_KEY}" \
  --header "X-Loom-TenantID:${LOOM_TENANT}" \
  --connect-timeout ${TIMEOUT} \
  ${LOOM_SERVER_URI}/v2/plugins/sync
__ret=$?
[[ ${__ret} -ne 0 ]] && exit 1
exit 0
