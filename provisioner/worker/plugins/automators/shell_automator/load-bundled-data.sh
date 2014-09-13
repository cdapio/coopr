#!/usr/bin/env bash

TIMEOUT=3
LOOM_HOME=${LOOM_HOME:-/opt/loom}
LOOM_SERVER_URI=${LOOM_SERVER_URI:-http://localhost:55054}
LOOM_API_USER=${LOOM_API_USER:-admin}
LOOM_API_KEY=${LOOM_API_KEY:-1234567890abcdef}
LOOM_TENANT=${LOOM_TENANT:-superadmin}
SHELL_DIR=${LOOM_HOME}/provisioner/worker/plugins/automators/shell_automator

LOOM_RUBY=${LOOM_RUBY:-"${LOOM_HOME}/provisioner/embedded/bin/ruby"}
test -x ${LOOM_RUBY} || LOOM_RUBY="ruby"
DATA_UPLOADER="${LOOM_RUBY} ${LOOM_HOME}/provisioner/bin/data-uploader.rb"

SCRIPTS_DIR=${SHELL_DIR}/scripts
ARCHIVES_DIR=${SHELL_DIR}/archives

# load scripts
cd ${SCRIPTS_DIR}
for f in $(ls -p | grep -v '/$' 2>/dev/null) ; do
  ${DATA_UPLOADER} --quiet --uri ${LOOM_SERVER_URI} --tenant ${LOOM_TENANT} \
    --user ${LOOM_API_USER} stage ${f} automatortypes/shell/scripts/${f}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${f}"
done
[[ ${failed} ]] && echo "Failed to load script: ${failed}" && exit 1

# load archives
cd ${ARCHIVES_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${LOOM_SERVER_URI} --tenant ${LOOM_TENANT} \
    --user ${LOOM_API_USER} stage ${d} automatortypes/shell/archives/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load data_bag: ${failed}" && exit 1

# We reached the end! Rejoice!
exit 0
