#!/usr/bin/env bash
#
# Copyright 2012-2014, Continuuity, Inc.
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

die ( ) {
  echo
  echo "$*"
  echo
  exit 1
}

APP_NAME="coopr-dummy-provisioner"
PID_DIR=${PID_DIR:-/var/run/coopr}
pid="${PID_DIR}/${APP_NAME}.pid"

check_before_start() {
  if [ ! -d "${PID_DIR}" ] ; then
    mkdir -p "${PID_DIR}"
  fi
  pid="${PID_DIR}/${APP_NAME}${p}.pid"
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

  echo "Starting Dummy Provisioner ..."
  nohup nice -1 java -cp server/lib/*:server/conf co.cask.coopr.runtime.MockProvisionerMain  >> ${COOPR_LOG_DIR}/${APP_NAME}${p}.log 2>&1 &
  pid="${PID_DIR}/${APP_NAME}${p}.pid"
  echo $! > $pid
}

stop ( ) {
  local failed=0
  echo "Stopping Dummy Provisioner ..."
  pid="${PID_DIR}/${APP_NAME}${p}.pid"
  if [ -f "${pid}" ] ; then
    echo -n "  Stopping provisioner ${p} ..."
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
          echo "  Provisioner ${p} (pid: $pidToKill) still running a task..."
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
  local failed=0
  pid="${PID_DIR}/${APP_NAME}${p}.pid"
  if [ -f $pid ]; then
    pidToCheck=`cat $pid`
    # kill -0 == see if the PID exists
    if kill -0 $pidToCheck > /dev/null 2>&1; then
      echo "${APP_NAME} ${p} running as process $pidToCheck"
      ret=0
    else
      echo "${APP_NAME} ${p} pidfile exists, but process does not appear to be running"
      ret=3
    fi
  else
    echo "${APP_NAME} ${p} is not running"
    ret=2
  fi
  if [ ${ret} -ne 0 ] ; then
    failed=1
  fi
  if [ ${failed} -eq 0 ] ; then
    echo "Dummy Provisioner up and running"
  elif [ ${failed} -eq 3 ] ; then
    echo "At least one provisioner failed"
  fi
  return "${failed}"
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
    # no-op for dummy, assume they are loaded already
    ;;
  *)
    echo "Usage: $0 {start|stop|status|restart|register}"
    exit 1
    ;;
esac

exit $?
