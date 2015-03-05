#!/bin/sh

die ( ) { echo; echo "ERROR: ${*}"; echo; exit 1; }

COOPR_NODE=${COOPR_NODE:-node}
COOPR_NPM=${COOPR_NPM:-npm}
COOPR_GULP='./node_modules/gulp/bin/gulp.js'

${COOPR_NPM} run prebuild || die "npm failed"
${COOPR_NODE} ${COOPR_GULP} distribute || die "gulp failed"
