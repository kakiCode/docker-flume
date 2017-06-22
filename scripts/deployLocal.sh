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

docker stop $CONTAINER
docker rm $CONTAINER

echo "...setting up secrets in flume conf..."
cp $CONF_ORIG $CONF
echo "twitter-agent.sources.twitter-src.consumerSecret = $consumerSecret" >> $CONF
echo "twitter-agent.sources.twitter-src.accessTokenSecret = $accessTokenSecret" >> $CONF

docker build -t $IMG .

docker run -d --name $CONTAINER --env AGENT_NAME=$AGENT -v $base_folder/tmp:/var/log/flume $IMG 