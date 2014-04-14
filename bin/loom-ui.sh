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

LOOM_SERVER_URI=${LOOM_SERVER_URI:-http://localhost:55054}
LOOM_LOG_DIR=${LOOM_LOG_DIR:-/var/log/loom}
LOOM_UI_PORT=${LOOM_UI_PORT:-8100}
LOOM_HOME=${LOOM_HOME:-/opt/loom} ; export LOOM_HOME

die ( ) {
  echo
  echo "$*"
  echo
  exit 1
}

# Specifies UI Path
UI_PATH=${UI_PATH:-${LOOM_HOME}/ui}
ENVIRONMENT=${ENVIRONMENT:-production}

LOOM_NODE=${LOOM_NODE:-"${LOOM_HOME}/embedded/bin/node"}
APP_NAME="loom-ui"
PID_DIR=${PID_DIR:-/var/run/loom}
pid="${PID_DIR}/${APP_NAME}.pid"

check_before_start() {
  if [ ! -d "${PID_DIR}" ] ; then
    mkdir -p "${PID_DIR}"
  fi
  if [ -f "${pid}" ] ; then
    if kill -0 `cat $pid` > /dev/null 2>&1; then
      echo "$0 running as process `cat $pid`. Stop it first or use the restart function."
      exit 0
    fi
  fi
}

start ( ) {
  cd ${UI_PATH}
  check_before_start

  echo "Starting Loom UI ..."
  nohup nice -1 ${LOOM_NODE} ${UI_PATH}/server.js --env=${ENVIRONMENT} \
    --loomhost=${LOOM_SERVER_URI} --port=${LOOM_UI_PORT} \
    >> ${LOOM_LOG_DIR}/${APP_NAME}.log 2>&1 < /dev/null &
  echo $! > "${pid}"
}

stop ( ) {
  echo -n "Stopping Loom UI ..."
  if [ -f "${pid}" ] ; then
    pidToKill=`cat $pid`
    # kill -0 == see if the PID exists
    if kill -0 $pidToKill > /dev/null 2>&1; then
      kill $pidToKill > /dev/null 2>&1
      while kill -0 $pidToKill > /dev/null 2>&1 ; do
        echo -n .
        sleep 1
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
    fi
  fi
  return "${ret:-0}"
}

status() {
  if [ -f $pid ]; then
    pidToCheck=`cat $pid`
    # kill -0 == see if the PID exists
    if kill -0 $pidToCheck > /dev/null 2>&1; then
      echo "${APP_NAME} running as process $pidToCheck"
      return 0
    else
      echo "${APP_NAME} pidfile exists, but process does not appear to be running"
      return 3
    fi
  else
    echo "${APP_NAME} is not running"
    return 3
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
  *)
    echo "Usage: $0 {start|stop|status|restart}"
    exit 1
    ;;
esac

exit $?
