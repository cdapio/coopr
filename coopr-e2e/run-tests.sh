#!/usr/bin/env bash

SOURCE="${BASH_SOURCE[0]}"
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

COOPR_HOME=${COOPR_HOME:-"$(dirname "$DIR")"};
SERVER_PORT=55054
SERVER_DIR="coopr-server"
E2E_DIR="coopr-e2e"
UI_DIR="coopr-ngui"
COOPR_SERVER_CONF=${COOPR_SERVER_CONF:-"$COOPR_HOME/$E2E_DIR/config"}
CLASSPATH="${COOPR_HOME}/${SERVER_DIR}/target/*:${COOPR_SERVER_CONF}"
MOCK_MAIN_CLASS="co.cask.coopr.runtime.MockProvisionerMain"
MAIN_CLASS="co.cask.coopr.runtime.ServerMain"
PID_DIR=${PID_DIR:-/var/run/coopr}

serverpid="${PID_DIR}/coopr-server.pid"
mockpid="${PID_DIR}/mock-provisioner.pid"
uipid="${PID_DIR}/ui.pid"

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

# Check if servers are running before starting.
if [ ! -d "${PID_DIR}" ] ; then
  mkdir -p "${PID_DIR}"
fi
if [ -f "${serverpid}" ] ; then
  if kill -0 `cat $serverpid` > /dev/null 2>&1; then
    die "$0 running as process `cat $serverpid`. Stop it first or use the restart function."
  fi
fi
if [ -f "${mockpid}" ] ; then
  if kill -0 `cat $mockpid` > /dev/null 2>&1; then
    die "$0 running as process `cat $mockpid`. Stop it first or use the restart function."
  fi
fi
if [ -f "${uipid}" ] ; then
  if kill -0 `cat $uipid` > /dev/null 2>&1; then
    die "$0 running as process `cat $uipid`. Stop it first."
  fi
fi

echo $COOPR_HOME

if [[ $DIR == *$E2E_DIR* ]]; then
  pushd ../
fi

pushd $UI_DIR
echo "Starting UI"
npm run build > /dev/null 2>&1 || die 'Problem building UI'
nohup nice -1 npm start 2>&1 < /dev/null &
echo $! > $uipid

popd && pushd $SERVER_DIR

echo "Building Server"
mvn clean package assembly:single -DskipTests
popd

echo "Starting Server"
echo $JAVACMD
echo $CLASSPATH
nohup nice -1 "$JAVACMD" -classpath "$CLASSPATH" ${MAIN_CLASS} \
  2>&1 < /dev/null &
echo $! > $serverpid

echo "Starting Mock Provisioner"
nohup nice -1 "$JAVACMD" -classpath "$CLASSPATH" ${MOCK_MAIN_CLASS} -p "$SERVER_PORT" \
  2>&1 < /dev/null &
echo $! > $mockpid

# give server generous time to start
sleep 10
pushd $E2E_DIR && npm run loadmock || { die 'Problem loading mocks'; }

popd && pushd $E2E_DIR
npm run protractor

echo -n "Stopping Servers"
if [ -f "${serverpid}" ] ; then
  pidToKill=`cat $serverpid`
  # kill -0 == see if the PID exists
  if kill -0 $pidToKill > /dev/null 2>&1; then
    kill $pidToKill > /dev/null 2>&1
    while kill -0 $pidToKill > /dev/null 2>&1 ; do
      echo -n .
      sleep 1
    done
    rm -f "${serverpid}"
    ret=0
  else
    ret=$?
  fi
  echo
  if [ ${ret} -eq 0 ] ; then
    echo "Stopped successfully"
  else
    echo "ERROR: Failed stopping!"
  fi
fi

if [ -f "${mockpid}" ] ; then
  pidToKill=`cat $mockpid`
  # kill -0 == see if the PID exists
  if kill -0 $pidToKill > /dev/null 2>&1; then
    kill $pidToKill > /dev/null 2>&1
    while kill -0 $pidToKill > /dev/null 2>&1 ; do
      echo -n .
      sleep 1
    done
    rm -f "${mockpid}"
    ret=0
  else
    ret=$?
  fi
  echo
  if [ ${ret} -eq 0 ] ; then
    echo "Stopped successfully"
  else
    echo "ERROR: Failed stopping!"
  fi
fi

echo "Stopping UI"
if [ -f "${uipid}" ] ; then
  pidToKill=`cat $uipid`
  # kill -0 == see if the PID exists
  if kill -0 $pidToKill > /dev/null 2>&1; then
    kill $pidToKill > /dev/null 2>&1
    while kill -0 $pidToKill > /dev/null 2>&1 ; do
      echo -n .
      sleep 1
    done
    rm -f "${uipid}"
    ret=0
  else
    ret=$?
  fi
  echo
  if [ ${ret} -eq 0 ] ; then
    echo "Stopped successfully"
  else
    echo "ERROR: Failed stopping!"
  fi
fi

exit $?






