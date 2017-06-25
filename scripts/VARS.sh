IMG=flume
CONTAINER=$IMG

AGENT=twitter-agent
CONF_ORIG=conf/flume.conf_ORIG
CONF=conf/flume.conf

BX_REGISTRY=registry.ng.bluemix.net/mynodeappbue
BX_IMG=$BX_REGISTRY/$IMG
BX_CONTAINER_MEMORY=128

KAFKA_CONTAINER=kafka
KAFKA_HOST=kafka

ZK_HOST=zookeeper
ZK_CONTAINER=zookeeper
