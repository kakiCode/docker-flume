## Apache Flume docker images

### Apache Flume extensions java project ( ``src`` folder )
The ``src`` folder contains custom extensions to Apache Flume sources and sinks, this java project is then used as an extension library to the diverse containers using Apache Flume.

### docker images (``docker`` folder)

- ``example`` folder : with old artifacts from previous experiences
- ``collector`` folder: docker image to collect data from kafka and dump it in influxdb
- ``percentage`` folder: docker image to generate random percentage values and dump it on kafka, for testing purposes;