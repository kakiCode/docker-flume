#!/bin/sh

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)

. $scripts_folder/VARS.sh
. $scripts_folder/include.sh

consumerSecret=$(getSecret consumerSecret)
accessTokenSecret=$(getSecret accessTokenSecret)

if [ -z $consumerSecret ] || [ -z $accessTokenSecret ] 
then
	echo "!!! secrets missing !!! ...leaving."
	return 1
fi

cf ic stop $CONTAINER
sleep 12
cf ic rm -f $CONTAINER

echo "...setting up secrets in flume conf..."
cp $CONF_ORIG $CONF
echo "twitter-agent.sources.twitter-src.consumerSecret = $consumerSecret" >> $CONF
echo "twitter-agent.sources.twitter-src.accessTokenSecret = $accessTokenSecret" >> $CONF

docker build -t $BX_IMG .
docker push $BX_IMG

cf ic run --name $CONTAINER --link $ZK_CONTAINER:$ZK_HOST --link $KAFKA_CONTAINER:$KAFKA_HOST --env AGENT_NAME=$AGENT -m $BX_CONTAINER_MEMORY $BX_IMG

sleep 12
cf ic logs $CONTAINER