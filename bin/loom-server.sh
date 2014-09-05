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

LOOM_LOG_DIR=${LOOM_LOG_DIR:-/var/log/loom}
LOOM_HOME=${LOOM_HOME:-/opt/loom} ; export LOOM_HOME

if [ -d /opt/loom ]; then
  DEFAULT_JVM_OPTS="-Xmx3072m"
else
  DEFAULT_JVM_OPTS="-Xmx1024m"
fi

die ( ) {
  echo
  echo "$*"
  echo
  exit 1
}

splitJvmOpts ( ) {
  JVM_OPTS=("$@")
}

APP_NAME="loom-server"
LOOM_SERVER_CONF=${LOOM_SERVER_CONF:-/etc/loom/conf}
CLASSPATH="${LOOM_HOME}/server/lib/*:${LOOM_SERVER_CONF}"
MAIN_CLASS="com.continuuity.loom.runtime.ServerMain"
PID_DIR=${PID_DIR:-/var/run/loom}
pid="${PID_DIR}/${APP_NAME}.pid"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
  fi
else
  JAVACMD="java"
  which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

source_function_library() {
  # Source function library. used for "status" use case
  if [ -f "/etc/rc.d/init.d/functions" ]; then
    PLATFORM="RHEL"
    . /etc/rc.d/init.d/functions
  elif [ -f /lib/lsb/init-functions ] ; then
    PLATFORM="UBUNTU"
    . /lib/lsb/init-functions
  else
    echo "Platform is unsupported."
    exit 0
  fi
}

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
  eval splitJvmOpts ${DEFAULT_JVM_OPTS} ${LOOM_JAVA_OPTS}
  check_before_start

  echo "Starting Loom Server ..."
  nohup nice -1 "$JAVACMD" "${JVM_OPTS[@]}" -classpath "$CLASSPATH" ${MAIN_CLASS} \
    >> ${LOOM_LOG_DIR}/${APP_NAME}.log 2>&1 < /dev/null &
  echo $! > $pid
}

stop ( ) {
  echo -n "Stopping Loom Server ..."
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
