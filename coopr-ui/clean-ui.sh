#!/bin/sh

COOPR_NODE=${COOPR_NODE:-node}
COOPR_GULP='./node_modules/gulp/bin/gulp.js'

${COOPR_NODE} ${COOPR_GULP} clean

rm -rf node_modules
rm -rf bower_components