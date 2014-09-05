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

COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
COOPR_LOG_DIR=${COOPR_LOG_DIR:-/var/log/coopr}
COOPR_LOG_LEVEL=${COOPR_LOG_LEVEL:-info}
COOPR_HOME=${COOPR_HOME:-/opt/coopr} ; export COOPR_HOME
PROVISIONER_SITE_CONF=${PROVISIONER_SITE_CONF:-/etc/coopr/conf/provisioner-site.xml}

die ( ) {
  echo
  echo "$*"
  echo
  exit 1
}

PROVISIONER_PATH="${COOPR_HOME}/provisioner/master"
PROVISIONER_CONF_OPT=""
if [ -f ${PROVISIONER_SITE_CONF} ] ; then
  PROVISIONER_CONF_OPT="--config ${PROVISIONER_SITE_CONF}"
fi

APP_NAME="coopr-provisioner"
COOPR_RUBY=${COOPR_RUBY:-"${COOPR_HOME}/embedded/bin/ruby"}
PID_DIR=${PID_DIR:-/var/run/coopr}
pid="${PID_DIR}/${APP_NAME}.pid"

check_before_start() {
  if [ ! -d "${PID_DIR}" ] ; then
    mkdir -p "${PID_DIR}"
  fi
  pid="${PID_DIR}/${APP_NAME}.pid"
  if [ -f "${pid}" ] ; then
    if kill -0 `cat $pid` > /dev/null 2>&1; then
      echo "$0 running as process `cat $pid`. Stop it first or use the restart function."
      exit 0
    fi
  fi
}

start ( ) {
  cd "${COOPR_HOME}"
  check_before_start

  # multi-provisioner
  echo "Starting Provisioner ..."
  nohup nice -1 ${COOPR_RUBY} ${PROVISIONER_PATH}/bin/provisioner ${PROVISIONER_CONF_OPT} \
    >> ${COOPR_LOG_DIR}/${APP_NAME}.log 2>&1 &
  pid="${PID_DIR}/${APP_NAME}${p}.pid"
  echo $! > $pid
}

register ( ) {
  echo "Registering provisioner plugins with configured server"
  nice -1 ${COOPR_RUBY} ${PROVISIONER_PATH}/bin/provisioner ${PROVISIONER_CONF_OPT} --register \
    >> ${COOPR_LOG_DIR}/${APP_NAME}.log 2>&1 &
}

stop ( ) {
  local failed=0
  echo -n "Stopping Provisioner ..."
  pid="${PID_DIR}/${APP_NAME}.pid"
  if [ -f "${pid}" ] ; then
    pidToKill=`cat $pid`
    # kill -0 == see if the PID exists
    if kill -0 $pidToKill > /dev/null 2>&1; then
      kill $pidToKill > /dev/null 2>&1
      local cnt=0
      while kill -0 $pidToKill > /dev/null 2>&1 ; do
        echo -n .
        sleep 1
        ((cnt++))
        if [ ${cnt} -ge 30 ]; then
          echo "  Provisioner (pid: $pidToKill) waiting for a worker task to complete..."
          break
        fi
      done
      rm -f "${pid}"
      ret=0
    else
      ret=$?
    fi
    echo
    if [ ${ret} -eq 0 ] ; then
      echo "Stopped successfully ..."
    else
      echo "ERROR: Failed stopping!"
      failed=1
    fi
  fi
  return "${failed}"
}

status() {
  pid="${PID_DIR}/${APP_NAME}.pid"
  if [ -f $pid ]; then
    pidToCheck=`cat $pid`
    if kill -0 $pidToCheck > /dev/null 2>&1; then
      echo "${APP_NAME} running as process $pidToCheck"
      return 0
    else
      echo "${APP_NAME} pidfile exists, but process does not appear to be running"
      return 3
    fi
  else
    echo "${APP_NAME} is not running"
    return 2
  fi
}

restart() {
  stop
  start
}

case ${1} in
  start)
    ${1}
    ;;
  stop)
    ${1}
    ;;
  status)
    ${1}
    ;;
  restart)
    ${1}
    ;;
  register)
    ${1}
    ;;
  *)
    echo "Usage: $0 {start|stop|status|restart|register}"
    exit 1
    ;;
esac

exit $?
