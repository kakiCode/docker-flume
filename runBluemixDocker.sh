#!/bin/sh

. ./VARS.sh

cf ic run --name $CONTAINER --env FLUME_AGENT_NAME=$FLUME_AGENT --link $KAFKA_CONTAINER:$KAFKA_CONTAINER_ALIAS --dns 8.8.8.8 --dns 8.8.4.4 -m $BX_CONTAINER_MEMORY $BX_IMG

sleep 6
cf ic logs $CONTAINER