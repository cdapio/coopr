#!/usr/bin/env bash

TIMEOUT=3
COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
COOPR_API_USER=${COOPR_API_USER:-admin}
COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
COOPR_TENANT=${COOPR_TENANT:-superadmin}
MAINDIR=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

dirs="plugins/providertypes plugins/automatortypes"

for d in ${dirs} ; do
  cd ${MAINDIR}
  [[ -d ${d} ]] && cd ${d} || continue
  for f in $(ls -1 *.json) ; do
    curl ${CURL_PARAMETER} --request PUT \
      --header "Content-Type:application/json" \
      --header "Coopr-UserID:${COOPR_API_USER}" \
      --header "Coopr-ApiKey:${COOPR_API_KEY}" \
      --header "Coopr-TenantID:${COOPR_TENANT}" \
      --connect-timeout ${TIMEOUT} --data @${f} \
      ${COOPR_SERVER_URI}/v2/${d}/${f/.json/} ${CERT_PARAMETER}
    ret=$?
    [[ ${ret} -ne 0 ]] && failed="${failed} ${d}/${f}"
  done
done
[[ ${failed} ]] && echo "Failed to load: ${failed}" && exit 1
exit 0
