#!/bin/sh

NAME=collector
IMG=$NAME
IMG_VERSION=latest
DOCKER_HUB_IMG=caquicode/$NAME


image_folder=$(dirname $(readlink -f $0))
docker_folder=$(dirname $image_folder)
base_folder=$(dirname $docker_folder)
target_folder=$base_folder/target
docker_lib_folder=$image_folder/lib
libjar=flume-extensions-1.0-SNAPSHOT.jar

echo "going to build image "


_pwd=`pwd`
cd $base_folder
mvn clean package
cp $target_folder/$libjar $docker_lib_folder/

echo "going to build image $IMG and push it to docker hub..."

cd $image_folder

docker rmi $IMG:$IMG_VERSION
docker build -t $IMG .
docker tag $IMG $DOCKER_HUB_IMG
docker push $DOCKER_HUB_IMG

cd $_pwd

echo "... done."