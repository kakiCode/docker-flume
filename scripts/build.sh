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


echo "...setting up flume conf..."
cp $docker_folder/$CONF_ORIG $docker_folder/$CONF

echo "going to build image $IMG and push it to docker hub..."

cd $docker_folder

docker rmi $IMG:$IMG_VERSION
docker build -t $IMG .
docker tag $IMG $DOCKER_HUB_IMG
docker push $DOCKER_HUB_IMG

cd $_pwd

echo "... done."