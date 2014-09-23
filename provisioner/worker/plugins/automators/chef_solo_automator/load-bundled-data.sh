#!/usr/bin/env bash

TIMEOUT=3
COOPR_HOME=${COOPR_HOME:-/opt/coopr}
COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
COOPR_API_USER=${COOPR_API_USER:-admin}
COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
COOPR_TENANT=${COOPR_TENANT:-superadmin}
CHEF_SOLO_DIR=${COOPR_HOME}/provisioner/worker/plugins/automators/chef_solo_automator/chef_solo_automator

COOPR_RUBY=${COOPR_RUBY:-"${COOPR_HOME}/provisioner/embedded/bin/ruby"}
test -x ${COOPR_RUBY} || COOPR_RUBY="ruby"
DATA_UPLOADER="${COOPR_RUBY} ${COOPR_HOME}/provisioner/bin/data-uploader.rb"

COOKBOOKS_DIR=${CHEF_SOLO_DIR}/cookbooks
ROLES_DIR=${CHEF_SOLO_DIR}/roles
DATA_BAGS_DIR=${CHEF_SOLO_DIR}/data_bags

# load cookbooks
cd ${COOKBOOKS_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${COOPR_SERVER_URI} --tenant ${COOPR_TENANT} \
    --user ${COOPR_API_USER} stage ${d} automatortypes/chef-solo/cookbooks/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load cookbook: ${failed}" && exit 1

# load data_bags
cd ${DATA_BAGS_DIR}
for d in $(ls -p -1 | grep "/$" | sed "s,/$,,") ; do
  ${DATA_UPLOADER} --quiet --uri ${COOPR_SERVER_URI} --tenant ${COOPR_TENANT} \
    --user ${COOPR_API_USER} stage ${d} automatortypes/chef-solo/data_bags/${d}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${d}"
done
[[ ${failed} ]] && echo "Failed to load data_bag: ${failed}" && exit 1

# load roles
cd ${ROLES_DIR}
for f in $(ls -1 {*.rb,*.json} 2>/dev/null) ; do
  ${DATA_UPLOADER} --quiet --uri ${COOPR_SERVER_URI} --tenant ${COOPR_TENANT} \
    --user ${COOPR_API_USER} stage ${f} automatortypes/chef-solo/roles/${f}
  ret=$?
  [[ ${ret} -ne 0 ]] && failed="${failed} ${f}"
done
[[ ${failed} ]] && echo "Failed to load role: ${failed}" && exit 1

# We reached the end! Rejoice!
exit 0
