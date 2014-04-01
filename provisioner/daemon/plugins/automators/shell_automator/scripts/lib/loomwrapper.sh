#!/bin/bash

# Loom wrapper script to invoke user-specified shell provisioner scripts via TickTick (https://github.com/kristopolous/TickTick)
# This provides some basic json value lookup capabilities which the shell provisioner can use to lookup configuration values.
# Implementation is a bit hacky

# Loom shell provisioner must invoke wrapper with the following first two args:
#   - full path to task json
#   - relative path given by user
TASKJSON=$1

# pass all args to loomticktick, which has been customized to handle these first two args, and pass remaining thru to the user script
. ./lib/loomticktick.sh $@

TASKDATA=`cat $TASKJSON`
tickParse "$TASKDATA"
