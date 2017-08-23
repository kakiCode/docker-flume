# docker-flume
## Apache Flume docker image

- can be pulled from:
  - ```kakicode/flume```
  - ```registry.ng.bluemix.net/mynodeappbue/flume```
- hostname: flume
- expects to resolve :
  - zookeeper on "zookeeper" hostname
  - kafka on "kafka" hostname
  - influxdb on "influxdb" hostname
- provides java debug port on $DEBUG_PORT

### usage:

- edit VARS.sh accordingly:
  ```
  NAME=flume
  IMG=$NAME
  IMG_VERSION=latest
  CONTAINER=$IMG

  AGENT=kaki-agent
  CONF_ORIG=conf/flume.conf_ORIG
  CONF=conf/flume.conf

  BX_REGISTRY=registry.ng.bluemix.net/mynodeappbue
  BX_IMG=$BX_REGISTRY/$IMG
  BX_CONTAINER_MEMORY=128

  DOCKER_HUB_IMG=kakicode/$NAME

  KAFKA_CONTAINER=kafka
  KAFKA_HOST=kafka

  ZK_CONTAINER=zookeeper
  ZK_HOST=zookeeper

  INFLUXDB_CONTAINER=influxdb
  INFLUXDB_HOST=influxdb

  DEBUG_PORT=6006
  ```
- scripts/buildAndPushImage.sh - build docker image and push it to dockerHub (/kakicode/kafka) and private bluemix registry (registry.ng.bluemix.net/mynodeappbue/kafka)
- scripts/runLocal.sh - run on local docker engine
- scripts/runOnBluemix.sh - run on bluemix
- scripts/attachOnBluemix.sh - attach to bluemix bash process

