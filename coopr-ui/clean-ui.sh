#!/bin/sh

COOPR_NODE=${COOPR_NODE:-node}
COOPR_GULP='./node_modules/gulp/bin/gulp.js'

# Prefer the gulp task, fall back to delete
if [ -f ${COOPR_GULP} ]; then
  ${COOPR_NODE} ${COOPR_GULP} clean
else
  rm -rf dist
fi

rm -rf node_modules
rm -rf bower_components
