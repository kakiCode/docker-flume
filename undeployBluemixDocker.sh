#!/bin/sh

. ./VARS.sh

cf ic stop $CONTAINER
sleep 12
cf ic rm -f $CONTAINER