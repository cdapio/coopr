#!/bin/bash

RUNSCRIPT=$1
# ensure it exists, etc

# if ticktick enabled
  # pass all args, including runscript
  . loomticktick.sh $@

#. tickwrapper.sh userscript.sh
TASKDATA=`cat install.json`
tickParse "$TASKDATA"

echo "end of wrapper"


