#!/bin/sh

COOPR_NODE=`whereis node | awk '{print $2}'`
COOPR_NPM=`whereis npm | awk '{print $2}'`

rm -rf dist
${COOPR_NPM} run build && gulp distribute && ${COOPR_NPM} install --production
