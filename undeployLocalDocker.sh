#!/bin/sh

. ./VARS.sh

docker stop $CONTAINER
docker rm -f $CONTAINER