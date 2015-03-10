#!/usr/bin/env bash
#
# Copyright © 2012-2015 Cask Data, Inc.
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

export COOPR_LOG_DIR=${COOPR_LOG_DIR:-/var/log/coopr}
export COOPR_HOME=${COOPR_HOME:-/opt/coopr}

DEFAULT_JVM_OPTS=${DEFAULT_JVM_OPTS:--Xmx3072m}

die() { echo; echo "ERROR: ${*}"; echo; exit 1; }

splitJvmOpts() { JVM_OPTS=("$@"); }

APP_NAME="coopr-server"
COOPR_SERVER_CONF=${COOPR_SERVER_CONF:-/etc/coopr/conf}
CLASSPATH="${COOPR_HOME}/server/lib/*:${COOPR_SERVER_CONF}"
MAIN_CLASS="co.cask.coopr.runtime.ServerMain"
UPGRADE_CLASS="co.cask.coopr.upgrade.UpgradeTo0_9_9"
PID_DIR=${PID_DIR:-/var/run/coopr}
pid="${PID_DIR}/${APP_NAME}.pid"

# Determine the Java command to use to start the JVM.
if [ -n "${JAVA_HOME}" ] ; then
  if [ -x "${JAVA_HOME}/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="${JAVA_HOME}/jre/sh/java"
  else
    JAVACMD="${JAVA_HOME}/bin/java"
  fi
  if [ ! -x "${JAVACMD}" ] ; then
    die "JAVA_HOME is set to an invalid directory: ${JAVA_HOME}

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
  fi
else
  JAVACMD="java"
  which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

check_before_start() {
  [ -d ${PID_DIR} ] || mkdir -p ${PID_DIR}
  if [ -f ${pid} ] ; then
    if kill -0 `cat ${pid}` > /dev/null 2>&1; then
      echo "${0} running as process `cat ${pid}`. Stop it first or use the restart function."
      exit 0
    fi
  fi
}

start() {
  eval splitJvmOpts ${DEFAULT_JVM_OPTS} ${COOPR_JAVA_OPTS}
  check_before_start

  echo "Starting Server ..."
  nohup nice -1 ${JAVACMD} ${JVM_OPTS} -classpath ${CLASSPATH} ${MAIN_CLASS} \
    >> ${COOPR_LOG_DIR}/${APP_NAME}.log 2>&1 < /dev/null &
  echo ${!} > ${pid}
}

stop() {
  echo -n "Stopping Server ..."
  if [ -f ${pid} ] ; then
    pidToKill=`cat ${pid}`
    # kill -0 == see if the PID exists
    if kill -0 ${pidToKill} > /dev/null 2>&1; then
      kill ${pidToKill} > /dev/null 2>&1
      while kill -0 ${pidToKill} > /dev/null 2>&1 ; do
        echo -n .
        sleep 1
      done
      rm -f ${pid}
      ret=0
    else
      ret=${?}
    fi
    echo
    if [ ${ret} -eq 0 ] ; then
      echo "Stopped successfully ..."
    else
      echo "ERROR: Failed stopping!"
    fi
  fi
  return ${ret:-0}
}

status() {
  if [ -f ${pid} ]; then
    pidToCheck=`cat ${pid}`
    # kill -0 == see if the PID exists
    if kill -0 ${pidToCheck} > /dev/null 2>&1; then
      echo "${APP_NAME} running as process ${pidToCheck}"
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

restart() { stop; start; }

upgrade() {
  eval splitJvmOpts ${DEFAULT_JVM_OPTS} ${COOPR_JAVA_OPTS}
  check_before_start

  echo "Updating Coopr Server ... (this may take a while...)"
  nice -1 ${JAVACMD} ${JVM_OPTS} -classpath ${CLASSPATH} ${UPGRADE_CLASS} \
    >> ${COOPR_LOG_DIR}/${APP_NAME}.log 2>&1
  echo ${!} > ${pid}
}

case ${1} in
  start|stop|status|restart|upgrade) ${1} ;;
  *) echo "Usage: $0 {start|stop|status|restart}"; exit 1 ;;
esac

exit ${?}
