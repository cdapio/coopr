#!/usr/bin/env bash

TIMEOUT=3
LOOM_HOME=${LOOM_HOME:-/opt/loom}
LOOM_SERVER_URI=${LOOM_SERVER_URI:-http://localhost:55054}
LOOM_API_USER=${LOOM_API_USER:-admin}
LOOM_API_KEY=${LOOM_API_KEY:-1234567890abcdef}
LOOM_TENANT=${LOOM_TENANT:-superadmin}
CHEF_SOLO_DIR=${LOOM_HOME}/provisioner/worker/plugins/automators/chef_solo_automator/chef_solo_automator

LOOM_RUBY=${LOOM_RUBY:-"${LOOM_HOME}/provisioner/embedded/bin/ruby"}
test -x ${LOOM_RUBY} || LOOM_RUBY="ruby"
DATA_UPLOADER="${LOOM_RUBY} ${LOOM_HOME}/provisioner/bin/data-uploader.rb"

COOKBOOKS_DIR=${CHEF_SOLO_DIR}/cookbooks
ROLES_DIR=${CHEF_SOLO_DIR}/roles
DATA_BAGS_DIR=${CHEF_SOLO_DIR}/data_bags

# load cookbooks
cd ${COOKBOOKS_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${LOOM_SERVER_URI} --tenant ${LOOM_TENANT} \
    --user ${LOOM_API_USER} stage ${d} automatortypes/chef-solo/cookbooks/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load cookbook: ${failed}" && exit 1

# load data_bags
cd ${DATA_BAGS_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${LOOM_SERVER_URI} --tenant ${LOOM_TENANT} \
    --user ${LOOM_API_USER} stage ${d} automatortypes/chef-solo/data_bags/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load data_bag: ${failed}" && exit 1

# load roles
cd ${ROLES_DIR}
for f in $(ls -1 {*.rb,*.json} 2>/dev/null) ; do
  ${DATA_UPLOADER} --quiet --uri ${LOOM_SERVER_URI} --tenant ${LOOM_TENANT} \
    --user ${LOOM_API_USER} stage ${f} automatortypes/chef-solo/roles/${f}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${f}"
done
[[ ${failed} ]] && echo "Failed to load role: ${failed}" && exit 1
