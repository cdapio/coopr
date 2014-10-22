#!/usr/bin/env bash

TIMEOUT=3
COOPR_SERVER_HOME=${COOPR_SERVER_HOME:-/opt/coopr/server}
COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
COOPR_API_USER=${COOPR_API_USER:-admin}
COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
COOPR_TENANT=${COOPR_TENANT:-superadmin}
MAINDIR=${COOPR_SERVER_HOME}/config/defaults

dirs="clustertemplates hardwaretypes imagetypes providers services"

if [ "x$COOPR_USE_DUMMY_PROVISIONER" == "xtrue" ]
then
  dirs="$dirs plugins/providertypes plugins/automatortypes"
fi

for d in ${dirs} ; do
  cd ${MAINDIR}
  [[ -d ${d} ]] && cd ${d} || continue
  for f in $(ls -1 *.json) ; do
    curl $CURL_PARAMETER --silent --request PUT \
      --header "Content-Type:application/json" \
      --header "Coopr-UserID:${COOPR_API_USER}" \
      --header "Coopr-ApiKey:${COOPR_API_KEY}" \
      --header "Coopr-TenantID:${COOPR_TENANT}" \
      --connect-timeout ${TIMEOUT} --data @${f} \
      ${COOPR_SERVER_URI}/v2/${d}/${f/.json/} $CERT_PARAMETER
    ret=$?
    [[ ${ret} -ne 0 ]] && failed="${failed} ${d}/${f}"
  done
done
[[ ${failed} ]] && echo "Failed to load: ${failed}" && exit 1
exit 0
