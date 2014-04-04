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

TASKJSON=$1
shift
USERSCRIPT=$1
shift

# script location
_scriptdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# function to lookup keys in the given task json using https://github.com/rcrowley/json.sh
# issues:
#   - spaces in keys may break things
loom_lookup_key () {
  local _key=$1
  local _matches=`$_scriptdir/bin/json.sh <$TASKJSON | awk -v key=${_key} '{ if ($1 ~ key) print }'`
  local _nummatches=`echo "${_matches}" | wc -l`
  if [ "x$_matches" == "x" ]; then
    echo "key not found" 1>&2;
    return 1
  elif [ $_nummatches -gt 1 ]; then
    # multiple matches found, is it an array?
    # check if each match also matches the key plus numerical index: $_key/[0-9]
    while read -r match; do
      local _arrkey
      # HACK HACK HACK - using a backspace to get rid of the extra OFS whenever the array element has spaces
      _arrkey=`echo ${match} | awk -v key=${_key}/[0-9] '{ if ($1 ~ key) { if ( NF>3) {ORS=" "; printf "\""} ; for(i=3;i<=NF;++i) print $i ; if ( NF>3) { printf "\b\""} } else exit 1 }'`
      if [ $? == 0 ]; then
        local _retarray=( "${_retarray[@]}" "$_arrkey" )
      else
        local _notanarray="true"
        break
      fi
    done <<< "${_matches}"
    if [ "x$_notanarray" != "x" ]; then
      echo "non-unique key, multiple matches found" 1>&2;
      return 1
    else
      # success; return space-delimited array values
      echo "${_retarray[@]}"
      return 0
    fi
  else
    # we have one unique match
    # to do: how to support spaces in keys, for now we take third field
    _value=`echo "${_matches}" | cut -d\  -f3- `
    echo "${_value}"
  fi
}

. $USERSCRIPT $*

