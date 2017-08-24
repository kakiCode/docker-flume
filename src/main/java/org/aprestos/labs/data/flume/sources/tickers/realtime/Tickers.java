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

package org.aprestos.labs.data.flume.sources.tickers.realtime;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.aprestos.labs.data.common.influxdb.PointDto;
import org.aprestos.labs.data.common.influxdb.PointUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Flume Source, which pulls data from Twitter's streaming API. Currently,
 * this only supports pulling from the sample API, and only gets new status
 * updates.
 */
public class Tickers extends AbstractSource implements EventDrivenSource, Configurable {

	private static final Logger logger = LoggerFactory.getLogger(Tickers.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	/** Information necessary for accessing the Twitter API */
	private String apikey;
	private String[] tickers;
	private int delayInMillis;
	private String interval;
	
	private ScheduledExecutorService service;

	/**
	 * The initialization method for the Source. The context contains all the
	 * Flume configuration info, and can be used to retrieve any configuration
	 * values necessary to set up the Source.
	 */
	@Override
	public void configure(Context context) {
		logger.debug("[IN]");
		if (0 == (delayInMillis = context.getInteger(Config.DELAY_IN_MILLIS))) {
			delayInMillis = Config.DEFAULT_DELAY;
			logger.warn("no delayInMillis specified using default {}", Config.DEFAULT_DELAY);
		}
		
		if (null == (interval = context.getString(Config.INTERVAL))) {
			interval = Config.DEFAULT_INTERVAL;
			logger.warn("no interval specified using default {}", Config.DEFAULT_INTERVAL);
		}
			
		if (null == (apikey = context.getString(Config.API_KEY))) 
			throw new RuntimeException("no apikey provided");
		
		String t = null;
		if (null == (t = context.getString(Config.TICKERS))) 
			throw new RuntimeException("no apikey provided");
		
		tickers = t.split(",");
		for(int i=0; i < tickers.length; i++)
			tickers[i] = tickers[i].trim();
		
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
			
			// The channel is the piece of Flume that sits between the Source and
					// Sink, and is used to process events.
			final ChannelProcessor channel = getChannelProcessor();
			
			for(String ticker:tickers){
				logger.info(String.format("starting runner for ticker %s", ticker));
				service.scheduleAtFixedRate(
						new Runner(getParameters(), ticker, channel)
						, this.delayInMillis, this.delayInMillis, TimeUnit.MILLISECONDS);
			}
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
		private static final String PROPERTY_PREFIX_TIME_SERIES = "Time Series (";
		
		private final Client client;
		private final ChannelProcessor channel;
		private final Map<String,String> parameters;
		private final String ticker;
		
		Runner(Map<String,String> parameters, String ticker, ChannelProcessor channel) throws NoSuchAlgorithmException, KeyManagementException{
			logger.debug("[IN]");
			this.channel = channel;
			this.parameters = parameters;
			this.ticker = ticker;
			TrustManager[] trustManager = new X509TrustManager[] { new X509TrustManager() {

			    @Override
			    public X509Certificate[] getAcceptedIssuers() {
			        return null;
			    }

			    @Override
			    public void checkClientTrusted(X509Certificate[] certs, String authType) {

			    }

			    @Override
			    public void checkServerTrusted(X509Certificate[] certs, String authType) {

			    }
			}};

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustManager, null);
			
			client = ClientBuilder.newBuilder().sslContext(sslContext).build();

			logger.debug("[OUT]");
		}
		
		@Override
		public void run() {
			logger.info("[IN]");
			try {
				WebTarget webTarget = client.target(Config.URI);
				for (Map.Entry<String,String> queryParam : parameters.entrySet())
					webTarget = webTarget.queryParam(queryParam.getKey(), queryParam.getValue());
				
				webTarget = webTarget.queryParam(Config.QUERY_PARAM_SYMBOL, ticker);
				
				Invocation.Builder invocationBuilder = webTarget.request();
				invocationBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE);
				invocationBuilder.accept(MediaType.APPLICATION_JSON_TYPE);
				Response response = invocationBuilder.buildGet().invoke();
				StatusType status = response.getStatusInfo();

				if (!status.getFamily().equals(Family.SUCCESSFUL)) {
					logger.info("call not successful");
					String errMessage = response.readEntity(String.class);
					logger.error(errMessage);
				}
				else {
					logger.info("call successful");
					String payload = response.readEntity(String.class);
					@SuppressWarnings({ "cast", "unchecked" })
					Map<String,Object> r = (Map<String,Object>)mapper.readValue(payload, Map.class);
					logger.info(r.toString());
					createPoints(r);
				}
			} catch (Exception e) {
				logger.error("oops", e);
			}
			finally {
				logger.info("[OUT]");
			}
			
		}
		
		private PointDto entry2PointDto(String measurement, long ts, Map<String, String> tags, Map<String, Double> fields) {
			logger.debug("[IN]");
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
				logger.debug("[OUT]");
			}

		}
		
		@SuppressWarnings("unchecked")
		private void createPoints(final Map<String,Object> data){
			logger.info("[IN]");
			DateTimeFormatter formatter = null;
			String ticker = null;
			final Map<String, String> headers = new HashMap<String, String>();
			
			try {
				for(Map.Entry<String, Object> entry: data.entrySet()){
					if(entry.getKey().startsWith(PROPERTY_PREFIX_TIME_SERIES)){
						Map<String,Object> dataEntries = (Map<String,Object>) entry.getValue();
						for(Map.Entry<String, Object> dataPoint: dataEntries.entrySet()){
							
							@SuppressWarnings("null")
							long ts = formatter.parseMillis(dataPoint.getKey());
							
							Map<String,String> tags = new HashMap<String,String>();
							tags.put("name", ticker);
							
							Map<String,Object> valueEntries = (Map<String,Object>) dataPoint.getValue();
							Map<String,Double> values = new HashMap<String,Double>();
							for(Map.Entry<String, Object> value: valueEntries.entrySet()){
								if( value.getKey().contains("open") )
									values.put("open", Double.valueOf((String)value.getValue()));
								if( value.getKey().contains("high") )
									values.put("high", Double.valueOf((String)value.getValue()));
								if( value.getKey().contains("low") )
									values.put("low", Double.valueOf((String)value.getValue()));
								if( value.getKey().contains("close") )
									values.put("close", Double.valueOf((String)value.getValue()));
								if( value.getKey().contains("volume") )
									values.put("volume", Double.valueOf((String)value.getValue()));
							}
						
							headers.put("timestamp", Long.toString(ts));
							PointDto point = entry2PointDto(Config.MEASUREMENT_TICKER, ts, tags, values);
							logger.info(String.format("processing point: %s", point.toString()));
							Event event = EventBuilder.withBody(PointUtils.toBytes(point), headers);
							channel.processEvent(event);
							
						}
					}
					else { //handle meta data
						Map<String,Object> metadataEntries = (Map<String,Object>) entry.getValue();
						for(Map.Entry<String, Object> metadata: metadataEntries.entrySet()){
							if( metadata.getKey().contains("Time Zone") ){
								formatter = new DateTimeFormatterBuilder()
										.appendYear(4, 4)
										.appendLiteral("-")
										.appendMonthOfYear(2)
										.appendLiteral("-")
										.appendDayOfMonth(2)
										.appendLiteral(" ")
										.appendHourOfDay(2)
										.appendLiteral(":")
										.appendMinuteOfHour(2)
										.appendLiteral(":")
										.appendSecondOfMinute(2)
										.toFormatter().withZone(DateTimeZone.forID((String)metadata.getValue()));
							}
							if( metadata.getKey().contains("Symbol") )
								ticker = (String)metadata.getValue();
						}

					}
				}
			} catch (Exception e) {
				logger.error("problem when trying to createPoints", e);
			}
			finally {
				logger.info("[OUT]");
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

	private Map<String,String> getParameters(){
		logger.debug("[IN]");
		Map<String, String> result;
		try {
			result = new HashMap<String, String>();
			result.put(Config.QUERY_PARAM_APIKEY, apikey);
			result.put(Config.QUERY_PARAM_FUNCTION, Config.FUNCTION);
			result.put(Config.QUERY_PARAM_INTERVAL, this.interval);
			return result;
		}
		finally {
			logger.debug("[OUT]");
		}
		
	}
	
	

}
