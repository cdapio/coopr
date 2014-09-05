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

LOOM_HOME=${LOOM_HOME:-/opt/loom}
LOOM_SERVER_CONF=${LOOM_SERVER_CONF:-/etc/loom/conf}
CLASSPATH="${LOOM_HOME}/server/lib/*:${LOOM_SERVER_CONF}"
MAIN_CLASS="com.continuuity.loom.tools.UpgradeTool"
APP_NAME="loom-server"
PID_DIR=${PID_DIR:-/var/run/loom}
pid="${PID_DIR}/${APP_NAME}.pid"

die ( ) {
  echo
  echo "$*"
  echo
  exit 1
}

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

check_before_start() {
  if [ -f "${pid}" ] ; then
    if kill -0 `cat $pid` > /dev/null 2>&1; then
      echo "$0 running as process `cat $pid`. It is not safe to upgrade while the server is running. Stop it first."
      exit 0
    fi
  fi
}

check_before_start

echo "Upgrading to Loom 0.9.8 ..."
$JAVACMD -Xmx1g -classpath $CLASSPATH $MAIN_CLASS

exit $?
