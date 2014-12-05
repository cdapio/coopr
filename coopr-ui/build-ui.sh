#!/bin/sh

COOPR_NODE=${COOPR_NODE:-node}
COOPR_NPM=${COOPR_NPM:-npm}
COOPR_GULP='./node_modules/gulp/bin/gulp.js'

${COOPR_NPM} run prebuild
${COOPR_NODE} ${COOPR_GULP} clean
${COOPR_NODE} ${COOPR_GULP} distribute
