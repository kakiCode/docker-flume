#!/bin/sh

. ./VARS.sh

cf ic stop $CONTAINER
sleep 6
cf ic rm -f $CONTAINER