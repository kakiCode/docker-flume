/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.aprestos.labs.data.flume.sources.dummies;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.aprestos.labs.data.common.influxdb.PointDto;
import org.aprestos.labs.data.common.influxdb.PointUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Dummy Flume Source, that generates random percentage values from 1 to 100. Configuration should be provided either
 * on the flume config for the source or in runtime environment variables. Configuration list: "measurement": the key,
 * also know as measurement, name to be created according to the influxdb line protocol;
 * 
 */
public class RandomPercentage extends AbstractSource implements EventDrivenSource, Configurable {

  private static final Logger logger = LoggerFactory.getLogger(RandomPercentage.class);

  protected static final Map<String, String> configuration = new HashMap<String, String>();

  protected static String getConfiguration(Config key) {
    return configuration.get(key.toString());
  }

  private ScheduledExecutorService service;

  /**
   * The initialisation method for the Source. The context and the environment should contain all the configuration
   * values necessary to set up the Source.
   */
  @Override
  public void configure(Context context) {
    logger.debug("configure|IN");

    List<String> configKeys = Stream.of(Config.values()).flatMap(o -> Stream.of(o.toString()))
        .collect(Collectors.toList());
    for (String k : configKeys) {
      String val = null;

      // precedence to env vars
      if (null == (val = System.getenv(k)))
        val = context.getString(k);

      if (null == val)
        throw new IllegalArgumentException(
            String.format("configure| !!! must provide % configuration, either in config or in env var !!!", k));

      logger.info(String.format("configure| found configuration %s: %s", k, val));
      configuration.put(k, val);
    }

    logger.debug("configure|OUT");
  }

  /**
   * Start processing events.
   */
  @Override
  public void start() {
    logger.debug("start|IN");
    int nProcessors = Runtime.getRuntime().availableProcessors();
    if (1 < nProcessors)
      nProcessors /= 2;

    try {
      service = Executors.newScheduledThreadPool(nProcessors, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r);
          t.setDaemon(true);
          return t;
        }
      });

      // The channel is the piece of Flume that sits between the Source and Sink, and is used to process events.
      final ChannelProcessor channel = getChannelProcessor();

      long delayInMillis = 0;
      try {
        delayInMillis = Long.parseLong(getConfiguration(Config.delayInMillis));
      } catch (Exception e) {
        throw new IllegalArgumentException(String.format("start|wrong config %s: %s", Config.delayInMillis.toString(),
            getConfiguration(Config.delayInMillis)));
      }

      service.scheduleAtFixedRate(new Runner(channel), delayInMillis, delayInMillis, TimeUnit.MILLISECONDS);

      super.start();
    } catch (Exception e) {
      logger.error("start| problem starting", e);
    } finally {
      logger.debug("start|OUT");
    }
  }

  /**
   * dummy source of data implementation
   * 
   * @author jviegas
   *
   */
  static final class Runner implements Runnable {

    private final ChannelProcessor channel;

    private final Random random;

    Runner(ChannelProcessor channel) {
      logger.debug("Runner|IN");
      this.channel = channel;
      this.random = new Random();
      logger.debug("Runner|OUT");
    }

    @Override
    public void run() {
      logger.debug("Runner|run|IN");
      try {
        double p = random.nextDouble() * 100;
        channel.processEvent(createPointEvent(p));
        logger.debug(String.format("Runner|run| new point: %s", Double.toString(p)));
      } catch (Exception e) {
        logger.error("Runner|run| oops", e);
      } finally {
        logger.debug("Runner|run|OUT");
      }

    }

  }

  /**
   * Stops the Source's event processing and shuts down the executor service.
   */
  @Override
  public void stop() {
    logger.debug("stop|IN");
    this.service.shutdown();
    super.stop();
    logger.debug("stop|OUT");
  }

  protected static Event createPointEvent(final double value) {
    logger.debug("createPointEvent|IN");
    Event result = null;
    try {
      final Map<String, String> headers = new HashMap<String, String>();
      long ts = new Date().getTime();
      Map<String, String> tags = new HashMap<String, String>();
      tags.put("name", "dummy_percentage");
      Map<String, Object> values = new HashMap<String, Object>();
      values.put("value", value);
      headers.put("timestamp", Long.toString(ts));
      PointDto point = new PointDto(ts, getConfiguration(Config.measurement), tags, values);
      logger.debug(String.format("createPointEvent|processing point: %s", point.toString()));
      result = EventBuilder.withBody(PointUtils.toBytes(point), headers);
    } catch (Exception e) {
      logger.error("createPointEvent|problem when trying to createPoints", e);
    } finally {
      logger.debug("createPointEvent|OUT");
    }
    return result;
  }

}
