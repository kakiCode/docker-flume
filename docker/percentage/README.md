## docker image for a source of random percentage values
### based on Apache Flume

- can be pulled from docker hub:
    - [caquicode/src-percent](https://hub.docker.com/r/caquicode/src-percent/)
    
- hostname: src-percent

- __writes random percentage values, in__ ``influx line format``__, and publishes them to__ ``source`` __topic in kafka__;

- expects:
    - to resolve zookeeper on ``zookeeper`` hostname, using default port;
    - to resolve kafka on ``kafka`` hostname, using default port;
  - environment variables to be provided in the container:
    - ``DELAY\_IN\_MILLIS`` - interval for generation of percentage dummy values;
    - ``MEASUREMENT`` - the influx line measurement/key/table name;
    - exposes optionally ``$DEBUG_PORT`` to attach to flume java process;

- scripts:
    - create.sh - build docker image and push it to dockerHub;
    - run.sh - runs an image container in the local docker engine;

- example docker-compose section:

	  src-percent:
	    image: caquicode/src-percent
	    container_name: src-percent
	    depends_on:
	      - kafka
	    links:
	      - "kafka:kafka"
	      - "zookeeper:zookeeper"
	    environment:
	      - DEBUG_PORT=6006
	      - DELAY_IN_MILLIS=5000
	      - MEASUREMENT=percent
	    ports:
	      - "6006:6006"


