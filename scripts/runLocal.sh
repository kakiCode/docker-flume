#!/bin/sh

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)

. $scripts_folder/VARS.sh

docker stop $CONTAINER
docker rm $CONTAINER

# docker run -d --name $CONTAINER --link $KAFKA_CONTAINER:$KAFKA_HOST --link $INFLUXDB_CONTAINER:$INFLUXDB_HOST --env AGENT_NAME=$AGENT -v $base_folder/tmp:/var/log/flume $IMG 
docker run -d -h $HOST --name $CONTAINER --link $KAFKA_CONTAINER:$KAFKA_HOST \
	--link $ZK_CONTAINER:$ZK_HOST \
	--link $INFLUXDB_CONTAINER:$INFLUXDB_HOST --env AGENT_NAME=$AGENT --env DEBUG_PORT=$DEBUG_PORT \
	-p $DEBUG_PORT:$DEBUG_PORT $DOCKER_HUB_IMG:$IMG_VERSION
# docker logs -f $CONTAINER
