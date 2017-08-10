#!/bin/sh

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)

. $scripts_folder/VARS.sh
. $scripts_folder/include.sh

consumerSecret=$(getSecret consumerSecret)
accessTokenSecret=$(getSecret accessTokenSecret)
apikey=$(getSecret apikey)

if [ -z $consumerSecret ] || [ -z $accessTokenSecret ] || [ -z $apikey ] 
then
	echo "!!! secrets missing !!! ...leaving."
	return 1
fi

docker stop $CONTAINER
docker rm $CONTAINER

echo "...setting up secrets in flume conf..."
cp $base_folder/$CONF_ORIG $base_folder/$CONF
echo "kaki-agent.sources.twitter-src.consumerSecret = $consumerSecret" >> $base_folder/$CONF
echo "kaki-agent.sources.twitter-src.accessTokenSecret = $accessTokenSecret" >> $base_folder/$CONF
echo "kaki-agent.sources.tickers-src.apikey = $apikey" >> $base_folder/$CONF

docker build -t $IMG $base_folder/

docker tag $IMG $BX_IMG
# docker run -d --name $CONTAINER --link $KAFKA_CONTAINER:$KAFKA_HOST --link $INFLUXDB_CONTAINER:$INFLUXDB_HOST --env AGENT_NAME=$AGENT -v $base_folder/tmp:/var/log/flume $IMG 
docker run -d --name $CONTAINER --link $KAFKA_CONTAINER:$KAFKA_HOST --link $INFLUXDB_CONTAINER:$INFLUXDB_HOST --env AGENT_NAME=$AGENT -p $DEBUG_PORT:$DEBUG_PORT $IMG
# docker logs -f $CONTAINER
