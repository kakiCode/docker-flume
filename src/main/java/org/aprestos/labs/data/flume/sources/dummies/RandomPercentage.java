/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aprestos.labs.data.flume.sources.dummies;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.aprestos.labs.data.common.influxdb.PointDto;
import org.aprestos.labs.data.common.influxdb.PointUtils;
import org.aprestos.labs.data.flume.sources.tickers.realtime.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Dummy Flume Source, that generates random percentage values from 1 to 100
 */
public class RandomPercentage extends AbstractSource implements EventDrivenSource, Configurable {

	private static final Logger logger = LoggerFactory.getLogger(RandomPercentage.class);
	
	private ScheduledExecutorService service;
	
	protected String[] configurationKeys = new String[]{"measurement"};
	protected String[] dynamicConfigurationKeys = new String[]{"delayInMillis"};
	protected final Map<String,String> configuration = new HashMap<String,String>();
	
	
	
	/**
	 * The initialization method for the Source. The context contains all the
	 * Flume configuration info, and can be used to retrieve any configuration
	 * values necessary to set up the Source.
	 */
	@Override
	public void configure(Context context) {
		logger.debug("[IN]");
		
		for(String ck: configurationKeys) {
			String val = null;
			if( null == ( val = context.getString(ck) ) )
				throw new IllegalArgumentException(String.format("!!! must provide % config !!!", ck));
			else 
				configuration.put(ck, val);
		}
		
		for(String dck: dynamicConfigurationKeys) {
			String val = null;
			if( null == ( val = System.getenv(dck) ) )
				throw new IllegalArgumentException(String.format("!!! must provide % dynamic config !!!", dck));
			else 
				configuration.put(dck, val);
		}
			
		logger.debug("[OUT]");
	}


	/**
	 * Start processing events.
	 */
	@Override
	public void start() {
		logger.info("[IN]");
		int nProcessors = Runtime.getRuntime().availableProcessors();
		if(1 < nProcessors)
			nProcessors/=2;
		
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
				delayInMillis = Long.parseLong(configuration.get("delayInMillis"));
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(" wrong config %s: %s", "delayInMillis", configuration.get("delayInMillis")));
			}
			
			service.scheduleAtFixedRate(
					new Runner(channel)
					, delayInMillis, delayInMillis, TimeUnit.MILLISECONDS);

			super.start();
		} catch (Exception e) {
			logger.error("problem starting", e);
		}
		finally {
			logger.info("[OUT]");
		}
	}
	
	
	static final class Runner implements Runnable {
		private static final Logger logger = LoggerFactory.getLogger(Runner.class);
		
		private final ChannelProcessor channel;
		private final Random random;
		
		Runner(ChannelProcessor channel) throws NoSuchAlgorithmException, KeyManagementException{
			logger.debug("Runner [IN]");
			this.channel = channel;
			this.random = new Random();
			logger.debug("Runner [OUT]");
		}
		
		@Override
		public void run() {
			logger.info("run [IN]");
			try {
				createPoint(random.nextDouble() * 100);
			} catch (Exception e) {
				logger.error("oops", e);
			}
			finally {
				logger.info("run [OUT]");
			}
			
		}

		private void createPoint(final double value){
			logger.info("createPoint [IN]");
			try {
				final Map<String, String> headers = new HashMap<String, String>();
				long ts = new Date().getTime();
				Map<String,String> tags = new HashMap<String,String>();
				tags.put("name", "dummy_percentage");
				Map<String,Double> values = new HashMap<String,Double>();
				values.put("value", value);
				headers.put("timestamp", Long.toString(ts));
				PointDto point = entry2PointDto(Config.MEASUREMENT, ts, tags, values);
				logger.info(String.format("processing point: %s", point.toString()));
				Event event = EventBuilder.withBody(PointUtils.toBytes(point), headers);
				channel.processEvent(event);

			} catch (Exception e) {
				logger.error("problem when trying to createPoints", e);
			}
			finally {
				logger.info("createPoint [OUT]");
			}
		}
		
		private PointDto entry2PointDto(String measurement, long ts, Map<String, String> tags, Map<String, Double> fields) {
			logger.debug("entry2PointDto [IN]");
			try {
				PointDto result = new PointDto();
				result.setMeasurement(measurement);
				for(Map.Entry<String, String> tag: tags.entrySet())
					result.addTag(tag.getKey(), tag.getValue());
				
				for(Map.Entry<String, Double> field: fields.entrySet())
					result.addField(field.getKey(), field.getValue());

				result.setTimestamp(ts);

				return result;
			} 
			finally {
				logger.debug("entry2PointDto [OUT]");
			}
		}
		
	}
	
	
	
	/**
	 * Stops the Source's event processing and shuts down the Twitter stream.
	 */
	@Override
	public void stop() {
		logger.debug("[IN]");
		this.service.shutdown();
		super.stop();
		logger.debug("[OUT]");
	}

}
