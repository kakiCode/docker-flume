#!/bin/sh

. ./VARS.sh
. ./include.sh

consumerSecret=$(getSecret consumerSecret)
accessTokenSecret=$(getSecret accessTokenSecret)

if [ -z $consumerSecret ] || [ -z $accessTokenSecret ] 
then
	echo "!!! secrets missing !!! ...leaving."
	return 1
fi

echo "...setting up secrets in flume conf..."
#sed -i -- "s/.*consumerSecret = .*/twitter-agent\.sources.*\.twitter-src\.consumerSecret = $consumerSecret/g" $FLUME_CONF
#sed -i -- "s/.*consumerSecret = .*/twitter-agent\.sources.*\.twitter-src\.accessTokenSecret = $accessTokenSecret/g" $FLUME_CONF
cp $FLUME_CONF_ORIG $FLUME_CONF
echo "twitter-agent.sources.twitter-src.consumerSecret = $consumerSecret" >> $FLUME_CONF
echo "twitter-agent.sources.twitter-src.accessTokenSecret = $accessTokenSecret" >> $FLUME_CONF

docker build -t $BX_IMG .
docker push $BX_IMG

cf ic run --name $CONTAINER --env FLUME_AGENT_NAME=$FLUME_AGENT --link kafka:kafka --dns 8.8.8.8 --dns 8.8.4.4 -m $BX_CONTAINER_MEMORY $BX_IMG

sleep 6
cf ic logs $CONTAINER
