FROM java:openjdk-8-jre

RUN apt-get update && apt-get install -q -y --no-install-recommends wget

RUN mkdir -p /opt/flume/conf
RUN mkdir -p /opt/flume/bin
RUN mkdir -p /var/log/flume
RUN mkdir -p /opt/flume/plugins.d/twitter/lib

RUN wget -qO- http://archive.apache.org/dist/flume/1.6.0/apache-flume-1.6.0-bin.tar.gz \
  | tar zxvf - -C /opt/flume --strip 1

ADD start-flume.sh /opt/flume/bin/start-flume.sh
ADD flume.conf /opt/flume/conf/flume.conf
ADD flume-sources-1.0-SNAPSHOT.jar /opt/flume/plugins.d/twitter/lib/flume-sources-1.0-SNAPSHOT.jar

ENV PATH /opt/flume/bin:$PATH

ENV FLUME_CONF_FILE /opt/flume/conf/flume.conf
ENV FLUME_CONF_DIR /opt/flume/conf
#ENV FLUME_AGENT_NAME <must be provided> run docker run command option

CMD [ "start-flume.sh" ]
