NAME=flume
IMG=$NAME
IMG_VERSION=latest
CONTAINER=$IMG
HOST=$NAME

AGENT=kaki
CONF_ORIG=conf/flume_simple.conf
CONF=conf/flume.conf


DOCKER_HUB_IMG=kakicode/$NAME

#KAFKA_CONTAINER=kafka
#KAFKA_HOST=kafka
#ZK_CONTAINER=zookeeper
#ZK_HOST=zookeeper
#INFLUXDB_CONTAINER=influxdb
#INFLUXDB_HOST=influxdb

#DEBUG_PORT=6006
