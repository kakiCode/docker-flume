#!/bin/sh

echo "going to build image "

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)
docker_folder=$base_folder/docker
target_folder=$base_folder/target
docker_lib_folder=$docker_folder/lib
libjar=flume-extensions-1.0-SNAPSHOT.jar

. $scripts_folder/VARS.sh
. $scripts_folder/include.sh

_pwd=`pwd`
cd $base_folder
mvn clean package
cp $target_folder/$libjar $docker_lib_folder/

consumerSecret=$(getSecret consumerSecret)
accessTokenSecret=$(getSecret accessTokenSecret)
apikey=$(getSecret apikey)

if [ -z $consumerSecret ] || [ -z $accessTokenSecret ] || [ -z $apikey ] 
then
	echo "!!! secrets missing !!! ...leaving."
	return 1
fi

echo "...setting up secrets in flume conf..."
cp $docker_folder/$CONF_ORIG $docker_folder/$CONF
echo "kaki-agent.sources.tweets.consumerSecret = $consumerSecret" >> $docker_folder/$CONF
echo "kaki-agent.sources.tweets.accessTokenSecret = $accessTokenSecret" >> $docker_folder/$CONF
echo "kaki-agent.sources.tickers.apikey = $apikey" >> $docker_folder/$CONF

echo "going to build image $IMG and push it to docker hub and bluemix repository..."

cd $docker_folder

docker rmi $IMG:$IMG_VERSION
docker build -t $IMG $docker_folder
docker tag $IMG $DOCKER_HUB_IMG
docker tag $IMG $BLUEMIX_IMG
docker push $DOCKER_HUB_IMG
docker push $BLUEMIX_IMG

cd $_pwd

echo "... done."