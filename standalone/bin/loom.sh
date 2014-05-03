#!/usr/bin/env bash

##############################################################################
##
##  Loom Standalone start up script for *NIX and Mac
##
##############################################################################

# Server environment
# Add default JVM options here. You can also use JAVA_OPTS and LOOM_JAVA_OPTS to pass JVM options to this script.
export LOOM_JAVA_OPTS="-XX:+UseConcMarkSweepGC -Dderby.stream.error.field=DerbyUtil.DEV_NULL"

# UI environment
export ENVIRONMENT=local
export LOOM_NODE=${LOOM_NODE:-node}

# Provisioner environment
export LOOM_RUBY=${LOOM_RUBY:-ruby}

APP_NAME="loom-standalone"
APP_BASE_NAME=`basename "$0"`

function program_is_installed {
  # set to 0 initially
  local return_=0
  # set to 0 if not found
  type $1 >/dev/null 2>&1 || { local return_=1; }
  # return value
  return $return_
}

warn ( ) {
    echo "$*"
}

die ( ) {
    echo
    echo "$*"
    echo
    exit 1
}

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >&-
APP_HOME="`pwd -P`"

export PID_DIR=/var/tmp
export LOOM_HOME=$APP_HOME
export LOOM_SERVER_HOME=$APP_HOME/server
export LOOM_SERVER_CONF=$LOOM_HOME/server/conf/
export LOOM_LOG_DIR=$LOOM_HOME/logs
export LOOM_DATA_DIR=$LOOM_HOME/data

# Add Continuuity Loom embedded bin PATH, if it exists
if [ -d /opt/loom/embedded/bin ] ; then
    export PATH=/opt/loom/embedded/bin:${PATH}
fi

# Create log dir
mkdir -p $LOOM_LOG_DIR || die "Could not create dir $LOOM_LOG_DIR: $!"

# Create data dir
mkdir -p $LOOM_DATA_DIR || die "Could not create dir $LOOM_DATA_DIR: $!"
SED_LOOM_DATA_DIR=`echo $LOOM_DATA_DIR | sed 's:/:\\\/:g'`
sed -i.old "s/LOOM_DATA_DIR/$SED_LOOM_DATA_DIR/g" $LOOM_SERVER_CONF/loom-site.xml

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

# java version check
JAVA_VERSION=`java -version 2>&1 | grep "java version" | awk '{print $3}' | awk -F '.' '{print $2}'`
if [ $JAVA_VERSION -ne 6 ] && [ $JAVA_VERSION -ne 7 ]; then
  die "ERROR: Java version not supported
Please install Java 6 or 7 - other versions of Java are not yet supported."
fi

# Check node installation
program_is_installed node || die "Node.js is not installed
Please install Node.js - the minimum version supported v0.10.26."

# Check node version
NODE_VERSION=`node -v 2>&1`
NODE_VERSION_MAJOR=`echo $NODE_VERSION | awk -F '.' ' { print $2 } '`
NODE_VERSION_MINOR=`echo $NODE_VERSION | awk -F '.' ' { print $3 } '`
if [ $NODE_VERSION_MAJOR -lt 10 ]; then
  die "ERROR: Node.js version is not supported
The minimum version supported is v0.10.26."
elif [ $NODE_VERSION_MAJOR -eq 10 ] && [ $NODE_VERSION_MINOR -lt 26 ]; then
  die "ERROR: Node.js version is not supported
The minimum version supported is v0.10.26."
fi

# Check ruby installation
program_is_installed ruby || die "Ruby is not installed
Please install ruby - the minimum version supported v1.9.0p0."

# Check ruby version
RUBY_VERSION=`ruby -v 2>&1 | awk '{print $2}'`
RUBY_VERSION_MAJOR=`echo $RUBY_VERSION | awk -F '.' ' { print $1 } '`
RUBY_VERSION_MINOR=`echo $RUBY_VERSION | awk -F '.' ' { print $2 } '`
RUBY_VERSION_PATCH=`echo $RUBY_VERSION | awk -F '.' ' { print $3 } '`
if [ $RUBY_VERSION_MAJOR -lt 1 ]; then
  die "ERROR: Ruby version is not supported
The minimum version supported is v1.9.0p0"
elif [ $RUBY_VERSION_MAJOR -eq 1 ] && [ $RUBY_VERSION_MINOR -lt 9 ]; then
  die "ERROR: Ruby version is not supported
The minimum version supported is v1.9.0p0"
fi

# Load default configuration
function load_defaults () {
    if [ ! -f $LOOM_DATA_DIR/.load_defaults ]; then
        RETRIES=0
        echo "Waiting for server to start before loading default configuration..."
        # Wait for server to start
        until [[ $(curl http://localhost:55054/status 2> /dev/null | grep OK) || $RETRIES -gt 60 ]]; do
            sleep 2
            RETRIES=$[$RETRIES + 1]
        done

        if [ $RETRIES -gt 60 ]; then
            die "ERROR: Could not load defaults because server did not successfully start" 
        fi 

        echo "Loading default configuration..."
        $LOOM_HOME/server/config/defaults/load-defaults.sh && \
        touch $LOOM_DATA_DIR/.load_defaults

        # register the default plugins with the server
        $LOOM_HOME/bin/loom-provisioner.sh register

        echo
        echo "Go to http://localhost:8100. Have fun creating clusters!"
    fi
    
    return 0;
}

case "$1" in
  start)
    $LOOM_HOME/bin/loom-server.sh start && \
    $LOOM_HOME/bin/loom-ui.sh start && \
    $LOOM_HOME/bin/loom-provisioner.sh start && \
    load_defaults
  ;;

  stop)
    $LOOM_HOME/bin/loom-server.sh stop
    $LOOM_HOME/bin/loom-ui.sh stop
    $LOOM_HOME/bin/loom-provisioner.sh stop
  ;;

  restart)
    $LOOM_HOME/bin/loom-server.sh restart
    $LOOM_HOME/bin/loom-ui.sh restart
    $LOOM_HOME/bin/loom-provisioner.sh restart
  ;;

  status)
    $LOOM_HOME/bin/loom-server.sh status
    $LOOM_HOME/bin/loom-ui.sh status
    $LOOM_HOME/bin/loom-provisioner.sh status
  ;;

  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
  ;;


esac
exit $?
