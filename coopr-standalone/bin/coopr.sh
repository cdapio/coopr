#!/usr/bin/env bash

##############################################################################
##
##  Coopr Standalone start up script for *NIX and Mac
##
##############################################################################

# Server environment
# Add default JVM options here. You can also use JAVA_OPTS and COOPR_JAVA_OPTS to pass JVM options to this script.
export COOPR_JAVA_OPTS="-XX:+UseConcMarkSweepGC -Dderby.stream.error.field=DerbyUtil.DEV_NULL"

# UI environment
export ENVIRONMENT=local
export COOPR_NODE=${COOPR_NODE:-node}
export COOPR_NPM=${COOPR_NPM:-npm}
export COOPR_USE_NGUI=${COOPR_USE_NGUI:-false}
export COOPR_DISABLE_UI=${COOPR_DISABLE_UI:-false}

# Provisioner environment
export COOPR_RUBY=${COOPR_RUBY:-ruby}
export COOPR_USE_DUMMY_PROVISIONER=${COOPR_USE_DUMMY_PROVISIONER:-false}
export COOPR_API_USER=${COOPR_API_USER:-admin}
export COOPR_TENANT=${COOPR_TENANT:-superadmin}
export COOPR_NUM_WORKERS=${COOPR_NUM_WORKERS:-5}


APP_NAME="coopr-standalone"
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
export COOPR_HOME=$APP_HOME
export COOPR_SERVER_HOME=$APP_HOME/server
export COOPR_SERVER_CONF=$COOPR_HOME/server/conf/
export COOPR_PROVISIONER_CONF=$COOPR_HOME/provisioner/master/conf
export PROVISIONER_SITE_CONF=$COOPR_PROVISIONER_CONF/provisioner-site.xml
export COOPR_PROVISIONER_PLUGIN_DIR=$COOPR_HOME/provisioner/worker/plugins
export COOPR_LOG_DIR=$COOPR_HOME/logs
export COOPR_DATA_DIR=$COOPR_HOME/data

# Add embedded bin PATH, if it exists
if [ -d ${COOPR_HOME}/embedded/bin ] ; then
    export PATH=${COOPR_HOME}/embedded/bin:${PATH}
fi

# Create log dir
mkdir -p $COOPR_LOG_DIR || die "Could not create dir $COOPR_LOG_DIR: $!"

# Create data dir
mkdir -p $COOPR_DATA_DIR || die "Could not create dir $COOPR_DATA_DIR: $!"
SED_COOPR_DATA_DIR=`echo $COOPR_DATA_DIR | sed 's:/:\\\/:g'`
sed -i.old "s/COOPR_DATA_DIR/$SED_COOPR_DATA_DIR/g" $COOPR_SERVER_CONF/coopr-site.xml
sed -i.old "s/COOPR_DATA_DIR/$SED_COOPR_DATA_DIR/g" $COOPR_PROVISIONER_CONF/provisioner-site.xml

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

# Setup coopr configuration
COOPR_SERVER_URI=http://localhost:55054
line=`awk '/server.enable.ssl/{print NR; exit}' ${COOPR_SERVER_CONF}coopr-site.xml`
line=$((line+1))
ssl=`sed -n "${line}p" ${COOPR_SERVER_CONF}coopr-site.xml | awk -F"<|>" '{print $3}'`

if [ ! -z $ssl ] && [ $ssl = "true" ]; then
  COOPR_SERVER_URI=https://localhost:55054
fi


# Load default configuration
function load_defaults () {
    shift;
    if [ ! -f $COOPR_DATA_DIR/.load_defaults ]; then
        echo "Waiting for server to start before loading default configuration..."
        wait_for_server

        echo "Loading default configuration..."
        $COOPR_HOME/server/config/defaults/load-defaults.sh && \
        touch $COOPR_DATA_DIR/.load_defaults

        # register the default plugins with the server
        provisioner register

        # load the initial plugin bundled data
        stage_default_data

        # sync the initial data to the provisioner
        sync_default_data

        # add some workers to the superadmin tenant
        request_superadmin_workers
    else
        return 0
    fi
}

function stage_default_data () {
    echo "Waiting for plugins to be registered..."
    wait_for_plugin_registration

    cd ${COOPR_PROVISIONER_PLUGIN_DIR}
    echo "Loading initial data..."
    for script in $(ls -1 */*/load-bundled-data.sh) ; do
      ${COOPR_PROVISIONER_PLUGIN_DIR}/${script}
    done
}

function sync_default_data () {
    echo "Syncing initial data..."
    curl --insecure --silent --request POST \
      --header "Coopr-UserID:${COOPR_API_USER}" \
      --header "Coopr-TenantID:${COOPR_TENANT}" \
      --connect-timeout 5 \
      $COOPR_SERVER_URI/v2/plugins/sync
}

function request_superadmin_workers () {

    if [ "x${COOPR_USE_DUMMY_PROVISIONER}" != "xtrue" ]; then
        wait_for_provisioner
    else
        sleep 5
    fi

    echo "Requesting ${COOPR_NUM_WORKERS} workers for default tenant..."
    curl --insecure --silent --request PUT \
      --header "Content-Type:application/json" \
      --header "Coopr-UserID:${COOPR_API_USER}" \
      --header "Coopr-TenantID:${COOPR_TENANT}" \
      --connect-timeout 5 --data "{ \"tenant\":{\"workers\":${COOPR_NUM_WORKERS}, \"name\":\"superadmin\"} }" \
      $COOPR_SERVER_URI/v2/tenants/superadmin
}

function wait_for_server () {
    RETRIES=0
    until [[ $(curl --insecure $COOPR_SERVER_URI/status 2> /dev/null | grep OK) || $RETRIES -gt 60 ]]; do
        sleep 2
        ((RETRIES++))
    done

    if [ $RETRIES -gt 60 ]; then
        die "ERROR: Server did not successfully start"
    fi
}

function wait_for_plugin_registration () {
    RETRIES=0
    until [[ $(curl --insecure --silent --request GET \
                 --output /dev/null --write-out "%{http_code}" \
                 --header "Coopr-UserID:${COOPR_API_USER}" \
                 --header "Coopr-TenantID:${COOPR_TENANT}" \
                 $COOPR_SERVER_URI/v2/plugins/automatortypes/chef-solo 2> /dev/null) -eq 200 || $RETRIES -gt 60 ]]; do
        sleep 2
echo "status failure"
        ((RETRIES++))
    done

    if [ $RETRIES -gt 60 ]; then
        die "ERROR: Provisioner did not successfully register plugins"
    fi
}

function wait_for_provisioner () {
    RETRIES=0
    until [[ $(curl --insecure https://localhost:55056/status 2> /dev/null | grep OK) || $RETRIES -gt 60 ]]; do
        sleep 2
        ((RETRIES++))
    done

    if [ $RETRIES -gt 60 ]; then
        die "ERROR: Provisioner did not successfully start"
    fi
}

function provisioner () {
    if [ "x$1" == "xstart" ]
    then
        echo "Waiting for server to start before running provisioner..."
        wait_for_server
    fi
    if [ "x${COOPR_USE_DUMMY_PROVISIONER}" == "xtrue" ]
    then
        $COOPR_HOME/server/bin/dummy-provisioner.sh $@
    else
        $COOPR_HOME/provisioner/bin/provisioner.sh $1
    fi
}

function ui () {
    if [ "x${COOPR_DISABLE_UI}" == "xtrue" ]
    then
        echo "UI disabled... skipping..."
        return 0
    fi
    if [ "x${COOPR_USE_NGUI}" == "xtrue" ]
    then
        $COOPR_HOME/ngui/bin/ngui.sh $1
    else
        $COOPR_HOME/ui/bin/ui.sh $1
    fi
}

function greeting () {
    [ "x${COOPR_DISABLE_UI}" == "xtrue" ] && return 0
    echo
    echo "Go to https://localhost:8100. Have fun creating clusters!"
}

case "$1" in
  start)
    $COOPR_HOME/server/bin/server.sh start && \
    ui start && \
    provisioner start && \
    load_defaults && \
    greeting
  ;;

  stop)
    provisioner stop
    $COOPR_HOME/server/bin/server.sh stop
    ui stop
  ;;

  restart)
    provisioner stop
    ui stop
    $COOPR_HOME/server/bin/server.sh stop
    $COOPR_HOME/server/bin/server.sh start
	ui start
    provisioner start
  ;;

  status)
    $COOPR_HOME/server/bin/server.sh status
    ui status
    provisioner status
  ;;

  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
  ;;


esac
exit $?