FROM java:openjdk-8-jre

ENV FLUME_DIR /opt/flume
ENV CONF_DIR $FLUME_DIR/conf
ENV CONF_FILE $CONF_DIR/flume.conf
ENV BIN_DIR $FLUME_DIR/bin
ENV LOG_DIR /var/log/flume

ENV PLUGINS_DIR $FLUME_DIR/plugins.d
ENV TWITTER_PLUGIN_SRC flume-sources-1.0-SNAPSHOT.jar
ENV TWITTER_PLUGIN_DIR $PLUGINS_DIR/twitter/lib

ENV FLUME_BUNDLE http://archive.apache.org/dist/flume/1.6.0/apache-flume-1.6.0-bin.tar.gz


RUN apt-get update && apt-get install -q -y --no-install-recommends wget

RUN mkdir -p $CONF_DIR
RUN mkdir -p $BIN_DIR
RUN mkdir -p $LOG_DIR
RUN mkdir -p $TWITTER_PLUGIN_DIR

RUN wget -qO- $FLUME_BUNDLE | tar zxvf - -C /opt/flume --strip 1

ADD start-flume.sh $BIN_DIR/start-flume.sh
ADD conf/flume.conf $CONF_FILE
ADD lib/$TWITTER_PLUGIN_SRC $TWITTER_PLUGIN_DIR/$TWITTER_PLUGIN_SRC

ENV PATH /opt/flume/bin:$PATH

#ENV AGENT_NAME <must be provided> run docker run command option setting env var
CMD [ "start-flume.sh" ]
