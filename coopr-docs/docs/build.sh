#!/usr/bin/env bash

# Copyright © 2014-2015 Cask Data, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
  
# Build script for docs
# Builds the docs (all except javadocs) from the .rst source files using Sphinx
# Builds the javadocs and copies them into place
# Zips everything up so it can be staged

SCRIPT=`basename $0`

SOURCE="source"
BUILD="build"
HTML="html"
API="coopr-server"
APIDOCS="apidocs"
JAVADOCS="javadocs"
LICENSES="licenses"
PROJECT="coopr"
PROJECT_CAPS="Coopr"

SCRIPT_PATH=`pwd`

SOURCE_PATH="$SCRIPT_PATH/$SOURCE"

BUILD_PATH="$SCRIPT_PATH/$BUILD"
HTML_PATH="$BUILD_PATH/$HTML"

LICENSES_SOURCE="$SCRIPT_PATH/../../$LICENSES"

if [ "x$2" == "x" ]; then
  PROJECT_PATH="$SCRIPT_PATH/../../"
else
  PROJECT_PATH="$2"
fi
PROJECT_JAVADOCS="$PROJECT_PATH/target/site/apidocs"
SDK_JAVADOCS="$PROJECT_PATH/$API/target/site/$APIDOCS"

ZIP_FILE_NAME=$HTML
ZIP="$ZIP_FILE_NAME.zip"

# Set Google Analytics Codes
# Corporate Docs Code
GOOGLE_ANALYTICS_WEB="UA-55077523-3"
WEB="web"
# Coopr Project Code
GOOGLE_ANALYTICS_GITHUB="UA-55081520-3"
GITHUB="github"

REDIRECT_EN_HTML=`cat <<EOF
<!DOCTYPE HTML>
<html lang="en-US">
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0;url=en/index.html">
        <script type="text/javascript">
            window.location.href = "en/index.html"
        </script>
        <title></title>
    </head>
    <body>
    </body>
</html>
EOF`

function usage() {
  cd $PROJECT_PATH
  PROJECT_PATH=`pwd`
  echo "Build script for '$PROJECT_CAPS' docs"
  echo "Usage: $SCRIPT < option > [source]"
  echo ""
  echo "  Options (select one)"
  echo "    all           Clean build of everything: HTML docs and Javadocs, GitHub and Web versions"
  echo "    build         Clean build of javadocs and HTML docs, copy javadocs and PDFs into place, zip results"
  echo "    build-github  Clean build and zip for placing on GitHub"
  echo "    build-web     Clean build and zip for placing on docs.cask.co webserver"
  echo ""
  echo "    docs          Clean build of docs"
  echo "    javadocs      Clean build of javadocs (api module only) for SDK and website"
  echo "    javadocs-full Clean build of javadocs for all modules"
  echo "    zip           Zips docs into $ZIP"
  echo ""
  echo "    depends       Build Site listing dependencies"
  echo "    sdk           Build SDK"
  echo ""
  echo "  with"
  echo "    source        Path to $PROJECT source for javadocs, if not $PROJECT_PATH"
  echo ""
  exit 1
}

function run_command() {
  case "$1" in
    all )               build_all; exit 1;;
    build )             build; exit 1;;
    build-web )         build_web; exit 1;;
    build-github )      build_github; exit 1;;
    docs )              build_docs; exit 1;;
    build-standalone )  build_standalone; exit 1;;
    copy-javadocs )     copy_javadocs_sdk; exit 1;;
    copy-licenses )     copy_licenses; exit 1;;
    javadocs )          build_javadocs_sdk; exit 1;;
    javadocs-full )     build_javadocs_full; exit 1;;
    depends )           build_dependencies; exit 1;;
    sdk )               build_sdk; exit 1;;
    version )           print_version; exit 1;;
    test )              test; exit 1;;
    zip )               make_zip; exit 1;;
    * )                 usage; exit 1;;
  esac
}

function clean() {
  cd $SCRIPT_PATH
  rm -rf $SCRIPT_PATH/$BUILD
}

function build_docs() {
  clean
  cd $SCRIPT_PATH
  sphinx-build -b html -d build/doctrees source build/html
}

function build_docs_google() {
  clean
  cd $SCRIPT_PATH
  sphinx-build -D googleanalytics_id=$1 -D googleanalytics_enabled=1 -b html -d build/doctrees source build/html
}

function build_javadocs_full() {
  cd $PROJECT_PATH
  mvn clean site -DskipTests
}

function build_javadocs_sdk() {
  cd $PROJECT_PATH/$API
  mvn clean javadoc:javadoc -DskipTests
}

function copy_javadocs_sdk() {
  cd $BUILD_PATH/$HTML
  rm -rf $JAVADOCS
  cp -r $SDK_JAVADOCS .
  mv -f $APIDOCS $JAVADOCS
}

function copy_licenses() {
  cd $BUILD_PATH/$HTML
  mkdir $LICENSES
  cd $LICENSES
  cp $LICENSES_SOURCE/* .
}

function make_zip_html() {
  version
  ZIP_FILE_NAME="$PROJECT-docs-$PROJECT_VERSION.zip"
  cd $SCRIPT_PATH/$BUILD
  zip -r $ZIP_FILE_NAME $HTML/*
}

function make_zip() {
# This creates a zip that unpacks to the same name
  version
  if [ "x$1" == "x" ]; then
    ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION"
  else
    ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION-$1"
  fi
  cd $SCRIPT_PATH/$BUILD
  mv $HTML $ZIP_DIR_NAME
  zip -r $ZIP_DIR_NAME.zip $ZIP_DIR_NAME/*
}

function make_zip_localized() {
# This creates a named zip that unpacks to the Project Version, localized to english
  version
  ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION-$1"
  cd $SCRIPT_PATH/$BUILD
  mkdir $PROJECT_VERSION
  mv $HTML $PROJECT_VERSION/en
  zip -r $ZIP_DIR_NAME.zip $PROJECT_VERSION/*
}

function build_all() {
  echo "Not implemented"
}

function build() {
  build_docs
  build_javadocs_sdk
  copy_javadocs_sdk
  copy_licenses
  make_zip
}

function build_docs_google() {
  clean
  cd $SCRIPT_PATH
  sphinx-build -D googleanalytics_id=$1 -D googleanalytics_enabled=1 -b html -d build/doctrees source build/html
}

function build_web() {
# This is used to stage files
  build_docs_google $GOOGLE_ANALYTICS_WEB
  build_javadocs_sdk
  copy_javadocs_sdk
  make_zip_localized $WEB
}

function build_github() {
  # GitHub requires a .nojekyll file at the root to allow for Sphinx's directories beginning with underscores
  build_docs_google $GOOGLE_ANALYTICS_GITHUB
  build_javadocs_sdk
  copy_javadocs_sdk
  make_zip $GITHUB
  ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION-$GITHUB"
  cd $SCRIPT_PATH/$BUILD
  touch $ZIP_DIR_NAME/.nojekyll
  zip $ZIP_DIR_NAME.zip $ZIP_DIR_NAME/.nojekyll
}


function build_standalone() {
  cd $PROJECT_PATH
  mvn clean package assembly:single -DskipTests
#   mvn clean package -DskipTests -P examples && mvn package -pl singlenode -am -DskipTests -P dist,release
}

function build_sdk() {
  echo "Not implemented"
}

function build_dependencies() {
  cd $PROJECT_PATH
  mvn clean package site -am -Pjavadocs -DskipTests
}

function version() {
  cd $PROJECT_PATH
#  PROJECT_VERSION=`mvn help:evaluate -o -Dexpression=project.version | grep -v '^\['`
  PROJECT_VERSION=`grep "<version>" pom.xml`
  PROJECT_VERSION=${PROJECT_VERSION#*<version>}
  PROJECT_VERSION=${PROJECT_VERSION%%</version>*}
  IFS=/ read -a branch <<< "`git rev-parse --abbrev-ref HEAD`"
  GIT_BRANCH="${branch[1]}"
}

function print_version() {
  version
  echo "PROJECT_PATH: $PROJECT_PATH"
  echo "PROJECT_VERSION: $PROJECT_VERSION"
  echo "GIT_BRANCH: $GIT_BRANCH"
}

function test() {
  echo "Test..."
  echo "Version..."
  print_version
  echo "Build all docs..."
  build
  echo "Test completed."
}

function start_standalone() {
  # pass in $1 path to unzipped standalone
  cd $1
  export COOPR_USE_DUMMY_PROVISIONER=true
  ./bin/coopr.sh start
  open http://localhost:8100/
}

if [ $# -lt 1 ]; then
  usage
  exit 1
fi

run_command  $1