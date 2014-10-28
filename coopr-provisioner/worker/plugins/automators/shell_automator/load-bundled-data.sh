#!/usr/bin/env bash

TIMEOUT=3
COOPR_HOME=${COOPR_HOME:-/opt/coopr}
COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
COOPR_API_USER=${COOPR_API_USER:-admin}
COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
COOPR_TENANT=${COOPR_TENANT:-superadmin}
SHELL_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

COOPR_RUBY=${COOPR_RUBY:-${COOPR_HOME}/provisioner/embedded/bin/ruby}
test -x ${COOPR_RUBY} || COOPR_RUBY="ruby"
DATA_UPLOADER="${COOPR_RUBY} ${COOPR_HOME}/provisioner/bin/data-uploader.rb"

SCRIPTS_DIR=${SHELL_DIR}/scripts
ARCHIVES_DIR=${SHELL_DIR}/archives

# load scripts
cd ${SCRIPTS_DIR}
for f in $(ls -p | grep -v '/$' 2>/dev/null) ; do
  ${DATA_UPLOADER} --quiet --uri ${COOPR_SERVER_URI} --tenant ${COOPR_TENANT} \
    --user ${COOPR_API_USER} stage ${f} automatortypes/shell/scripts/${f}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${f}"
done
[[ ${failed} ]] && echo "Failed to load script: ${failed}" && exit 1

# load archives
cd ${ARCHIVES_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${COOPR_SERVER_URI} --tenant ${COOPR_TENANT} \
    --user ${COOPR_API_USER} stage ${d} automatortypes/shell/archives/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load archive: ${failed}" && exit 1

# We reached the end! Rejoice!
exit 0
