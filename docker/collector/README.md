## docker image to transfer kafka topic data to influxdb
### based on Apache Flume

- can be pulled from docker hub:
    - [caquicode/collector](https://hub.docker.com/r/caquicode/collector/)
    
- hostname: collector

- __retrieves data from topic__ ``processed`` __in kafka and dumps it in influxdb database__ ``kaki``;

- expects:
    - to resolve zookeeper on ``zookeeper`` hostname, using default port;
    - to resolve kafka on ``kafka`` hostname, using default port;
    - to resolve influxdb on ``influxdb`` hostname, using default port;
  - environment variables to be provided in the container:
    - exposes optionally ``$DEBUG_PORT`` to attach to flume java process;

- scripts:
    - create.sh - build docker image and push it to dockerHub;
    - run.sh - runs an image container in the local docker engine;

- example docker-compose section:

	  collector:
	    image: caquicode/collector
	    container_name: collector
	    depends_on: 
	      - kafka
	      - influxdb
	    links:
	      - "zookeeper:zookeeper"
	      - "influxdb:influxdb"
	    environment:
	      - DEBUG_PORT=6007
	    ports:
	      - "6007:6007"


