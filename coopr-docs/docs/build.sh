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
# Builds the docs (all except javadocs and PDFs) from the .rst source files using Sphinx
# Builds the javadocs and copies them into place
# Zips everything up so it can be staged
# REST PDF is built as a separate target and checked in, as it is only used in SDK and not website
# Target for building the SDK
# Targets for both a limited and complete set of javadocs
# Targets not included in usage are intended for internal usage by script

API="coopr-server"
APIDOCS="apidocs"
BUILD="build"
BUILD_PDF="build-pdf"
BUILD_TEMP="build-temp"
HTACCESS="htaccess"
HTML="html"
JAVADOCS="javadocs"
LICENSES="licenses"
LICENSES_PDF="licenses-pdf"
PROJECT="coopr"
PROJECT_CAPS="Coopr"
SOURCE="source"

FALSE="false"
TRUE="true"

# Redirect placed in top to redirect to 'en' directory
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

DATE_STAMP=`date`
SCRIPT=`basename $0`
SCRIPT_PATH=`pwd`

SOURCE_PATH="$SCRIPT_PATH/$SOURCE"
BUILD_PATH="$SCRIPT_PATH/$BUILD"
HTML_PATH="$BUILD_PATH/$HTML"

DOCS_PY="$SCRIPT_PATH/../tools/doc-gen.py"

REST_SOURCE="$SOURCE_PATH/rest.rst"
REST_PDF="$SCRIPT_PATH/$BUILD_PDF/rest.pdf"

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

DOCS="docs"
DOCS_WEB="docs-$WEB"
DOCS_GITHUB="docs-$GITHUB"

ARG_1="$1"
ARG_2="$2"
ARG_3="$3"


function set_project_path() {
  if [ "x$ARG_2" == "x" ]; then
    PROJECT_PATH="$SCRIPT_PATH/../../"
  else
    PROJECT_PATH="$SCRIPT_PATH/../../../$ARG_2"
  fi
}

function usage() {
  cd $PROJECT_PATH
  PROJECT_PATH=`pwd`
  echo "Build script for '$PROJECT_CAPS' docs"
  echo "Usage: $SCRIPT < option > [source]"
  echo ""
  echo "  Options (select one)"
  echo "    all            Clean build of everything: HTML docs and Javadocs, GitHub and Web versions"
  echo ""
  echo "    docs           Clean build of just the HTML docs, skipping Javadocs, zipped for placing on docs.cask.co webserver"
  echo "    docs-github    Clean build of HTML docs and Javadocs, zipped for placing on GitHub"
  echo "    docs-web       Clean build of HTML docs and Javadocs, zipped for placing on docs.cask.co webserver"
  echo ""
  echo "    depends        Build Site listing dependencies"
  echo "    sdk            Build SDK"
  echo "    version        Print the version information"
  echo ""
  echo "  with"
  echo "    source         Path to $PROJECT source, if not $PROJECT_PATH"
  echo ""
}

function run_command() {
  case "$1" in
    all )               build_all; exit 1;;
    docs )              build_docs; exit 1;;
    docs-github )       build_docs_github; exit 1;;
    docs-web )          build_docs_web; exit 1;;
    depends )           build_dependencies; exit 1;;
    sdk )               build_sdk; exit 1;;
    version )           display_version; exit 1;;
    test )              test; exit 1;;
    * )                 usage; exit 1;;
  esac
}


function bell() {
  # Pass a message as $1
  echo -e "\a$1"
}

function build_all() {
  echo "Building GitHub Docs."
  ./build.sh docs-github $ARG_2 $ARG_3
  echo "Stashing GitHub Docs."
  cd $SCRIPT_PATH
  mkdir -p $SCRIPT_PATH/$BUILD_TEMP
  mv $SCRIPT_PATH/$BUILD/*.zip $SCRIPT_PATH/$BUILD_TEMP
  echo "Building Web Docs."
  ./build.sh docs-web $ARG_2 $ARG_3
  echo "Replacing GitHub Docs."
  mv $SCRIPT_PATH/$BUILD_TEMP/*.zip $SCRIPT_PATH/$BUILD
  rm -rf $SCRIPT_PATH/$BUILD_TEMP
  bell
}

# "docs" type doesn't build javadocs
#             Type         Google Analytics code    Target  Zip extras?
function build_docs() {
  _build_docs $DOCS        $GOOGLE_ANALYTICS_WEB    $WEB    $TRUE
}

function build_docs_github() {
  _build_docs $DOCS_GITHUB $GOOGLE_ANALYTICS_GITHUB $GITHUB $FALSE
}

function build_docs_web() {
  _build_docs $DOCS_WEB    $GOOGLE_ANALYTICS_WEB    $WEB    $TRUE
}

function _build_docs() {
  local type=$1
  local code=$2
  local target=$3
  local extras=$4
  echo ""
  echo "========================================================"
  echo "Building $type..."
  echo "========================================================"
  echo ""
  build $type $code
  build_zip $target
  zip_extras $extras
  display_version
  bell "Building $type completed."
}

function build() {
  local build_type=$1
  local googleanalytics_id=$2
  local googleanalytics=""
  if [ ! "x$googleanalytics_id" == "x" ]; then
    googleanalytics="-D googleanalytics_id=$googleanalytics_id -D googleanalytics_enabled=1"
  else
    echo "No Google Analytics code..."
  fi
  clean
  cd $SCRIPT_PATH
  sphinx-build $googleanalytics -b html -d build/doctrees source build/html
  echo
  if [ ! "x$build_type" == "x$DOCS" ]; then
    build_javadocs_sdk
    copy_javadocs_sdk
  else
    echo "Skipping building Javadocs..."
  fi
}

function build_zip() {
  echo "Building zip..."
  local target=$1
  cd $SCRIPT_PATH
  set_project_path
  version
  if [ "x$target" == "x" ]; then
    ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION"
  else
    ZIP_DIR_NAME="$PROJECT-docs-$PROJECT_VERSION-$1"
  fi
  cd $SCRIPT_PATH/$BUILD
  mkdir $PROJECT_VERSION
  mv $HTML $PROJECT_VERSION/en
  # Add a redirect index.html file
  echo "$REDIRECT_EN_HTML" > $PROJECT_VERSION/index.html
  # Zip everything
  zip -qr $ZIP_DIR_NAME.zip $PROJECT_VERSION/* --exclude .DS_Store
}

function zip_extras() {
  if [ "x$1" == "x$FALSE" ]; then
    echo "Not building extras..."
    return
  fi
  echo "Building extras..."
  # Add JSON file
  cd $SCRIPT_PATH/$SOURCE
  JSON_FILE=`python -c 'import conf; conf.print_json_versions_file();'`
  local json_file_path=$SCRIPT_PATH/$BUILD/$PROJECT_VERSION/$JSON_FILE
  echo `python -c 'import conf; conf.print_json_versions();'` > $json_file_path
  # Add .htaccess file (404 file)
  cd $SCRIPT_PATH
  rewrite $SOURCE/$HTACCESS $BUILD/$PROJECT_VERSION/.$HTACCESS "<version>" "$PROJECT_VERSION"
  cd $SCRIPT_PATH/$BUILD
  zip -qr $ZIP_DIR_NAME.zip $PROJECT_VERSION/$JSON_FILE $PROJECT_VERSION/.$HTACCESS
    
}

function clean() {
  cd $SCRIPT_PATH
  rm -rf $SCRIPT_PATH/$BUILD
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

function rewrite() {
  # Substitutes text in file $1 and outputting to file $2, replacing text $3 with text $4
  # or if $4=="", substitutes text in-place in file $1, replacing text $2 with text $3
  # or if $3 & $4=="", substitutes text in-place in file $1, using sed command $2
  cd $SCRIPT_PATH
  local rewrite_source=$1
  echo "Re-writing"
  echo "    $rewrite_source"
  if [ "x$3" == "x" ]; then
    local sub_string=$2
    echo "  $sub_string"
    if [ "$(uname)" == "Darwin" ]; then
      sed -i '.bak' "$sub_string" $rewrite_source
      rm $rewrite_source.bak
    else
      sed -i "$sub_string" $rewrite_source
    fi
  elif [ "x$4" == "x" ]; then
    local sub_string=$2
    local new_sub_string=$3
    echo "  $sub_string -> $new_sub_string "
    if [ "$(uname)" == "Darwin" ]; then
      sed -i '.bak' "s|$sub_string|$new_sub_string|g" $rewrite_source
      rm $rewrite_source.bak
    else
      sed -i "s|$sub_string|$new_sub_string|g" $rewrite_source
    fi
  else
    local rewrite_target=$2
    local sub_string=$3
    local new_sub_string=$4
    echo "  to"
    echo "    $rewrite_target"
    echo "  $sub_string -> $new_sub_string "
    sed -e "s|$sub_string|$new_sub_string|g" $rewrite_source > $rewrite_target
  fi
}

function build_sdk() {
  cd $PROJECT_PATH
  mvn clean package assembly:single -DskipTests
}

function build_dependencies() {
  cd $PROJECT_PATH
  mvn clean package site -am -Pjavadocs -DskipTests
}

function version() {
  cd $PROJECT_PATH
  PROJECT_VERSION=`grep "<version>" pom.xml`
  PROJECT_VERSION=${PROJECT_VERSION#*<version>}
  PROJECT_VERSION=${PROJECT_VERSION%%</version>*}
  PROJECT_SHORT_VERSION=`expr "$PROJECT_VERSION" : '\([0-9]*\.[0-9]*\)'`
  IFS=/ read -a branch <<< "`git rev-parse --abbrev-ref HEAD`"
  GIT_BRANCH_TYPE="${branch[0]}"
  GIT_BRANCH="${branch[1]}"
}

function display_version() {
  version
  echo ""
  echo "PROJECT_PATH: $PROJECT_PATH"
  echo "PROJECT_VERSION: $PROJECT_VERSION"
  echo "PROJECT_SHORT_VERSION: $PROJECT_SHORT_VERSION"
  echo "GIT_BRANCH_TYPE: $GIT_BRANCH_TYPE"
  echo "GIT_BRANCH: $GIT_BRANCH"
  echo ""
}

function test() {
  echo "Test..."
  echo "Version..."
  display_version "test"
  return
  echo "Build all docs..."
  build
  echo "Build SDK..."
  build_sdk
  echo "Test completed."
}

function start_standalone() {
  # pass in $1 path to unzipped standalone
  cd $1
  export COOPR_USE_DUMMY_PROVISIONER=true
  ./bin/coopr.sh start
  open http://localhost:8100/
}

set_project_path

run_command  $1
