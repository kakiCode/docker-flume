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

package org.aprestos.labs.data.flume.sources.tickers.history;

import java.util.HashMap;
import java.util.Map;

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

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * A Flume Source, which pulls data from Twitter's streaming API. Currently,
 * this only supports pulling from the sample API, and only gets new status
 * updates.
 */
public class Tickers extends AbstractSource implements EventDrivenSource, Configurable {

	public static final String CONSUMER_KEY_KEY = "consumerKey";
	public static final String CONSUMER_SECRET_KEY = "consumerSecret";
	public static final String ACCESS_TOKEN_KEY = "accessToken";
	public static final String ACCESS_TOKEN_SECRET_KEY = "accessTokenSecret";
	public static final String BATCH_SIZE_KEY = "batchSize";
	public static final long DEFAULT_BATCH_SIZE = 1000L;
	public static final String KEYWORDS_KEY = "keywords";
	public static final String POINT_MESAUREMENT = "tweet";
	private static final Logger logger = LoggerFactory.getLogger(Tickers.class);

	/** Information necessary for accessing the Twitter API */
	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessTokenSecret;

	private String[] keywords;

	/** The actual Twitter stream. It's set up to collect raw JSON data */
	private TwitterStream twitterStream;

	/**
	 * The initialization method for the Source. The context contains all the
	 * Flume configuration info, and can be used to retrieve any configuration
	 * values necessary to set up the Source.
	 */
	@Override
	public void configure(Context context) {
		consumerKey = context.getString(CONSUMER_KEY_KEY);
		consumerSecret = context.getString(CONSUMER_SECRET_KEY);
		accessToken = context.getString(ACCESS_TOKEN_KEY);
		accessTokenSecret = context.getString(ACCESS_TOKEN_SECRET_KEY);

		String keywordString = context.getString(KEYWORDS_KEY, "");
		if (keywordString.trim().length() == 0) {
			keywords = new String[0];
		} else {
			keywords = keywordString.split(",");
			for (int i = 0; i < keywords.length; i++) {
				keywords[i] = keywords[i].trim();
			}
		}

		try {
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setOAuthConsumerKey(consumerKey);
			cb.setOAuthConsumerSecret(consumerSecret);
			cb.setOAuthAccessToken(accessToken);
			cb.setOAuthAccessTokenSecret(accessTokenSecret);
			cb.setJSONStoreEnabled(true);
			cb.setIncludeEntitiesEnabled(true);

			twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		} catch (Exception e) {
			logger.error("configure", e);
		}
	}

	/**
	 * Start processing events. This uses the Twitter Streaming API to sample
	 * Twitter, and process tweets.
	 */
	@Override
	public void start() {
		// The channel is the piece of Flume that sits between the Source and
		// Sink,
		// and is used to process events.
		final ChannelProcessor channel = getChannelProcessor();

		final Map<String, String> headers = new HashMap<String, String>();

		// The StatusListener is a twitter4j API, which can be added to a
		// Twitter
		// stream, and will execute methods every time a message comes in
		// through
		// the stream.
		StatusListener listener = new StatusListener() {
			// The onStatus method is executed every time a new tweet comes in.
			public void onStatus(Status status) {

				// we'll only handle english language for now
				/*if(!status.getIsoLanguageCode().equalsIgnoreCase("en"))
					return;*/
				
				// The EventBuilder is used to build an event using the headers
				// and
				// the raw JSON of a tweet
				logger.debug(status.getUser().getScreenName() + ": " + status.getText());

				try {
					headers.put("timestamp", String.valueOf(status.getCreatedAt().getTime()));
					PointDto point = status2PointDto(status);
					logger.info(point.toString());
					Event event = EventBuilder.withBody(PointUtils.toBytes(point), headers);
					channel.processEvent(event);
				} catch (Exception e) {
					logger.error("onStatus", e);
				}
			}

			// This listener will ignore everything except for new tweets
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			}

			public void onScrubGeo(long userId, long upToStatusId) {
			}

			public void onException(Exception ex) {
			}

			public void onStallWarning(StallWarning warning) {
			}
		};

		logger.debug("Setting up Twitter sample stream using consumer key {} and" + " access token {}",
				new String[] { consumerKey, accessToken });
		// Set up the stream's listener (defined above),
		twitterStream.addListener(listener);

		// Set up a filter to pull out industry-relevant tweets
		if (keywords.length == 0) {
			logger.debug("Starting up Twitter sampling...");
			twitterStream.sample();
		} else {
			logger.debug("Starting up Twitter filtering...");

			FilterQuery query = new FilterQuery().track(keywords);
			twitterStream.filter(query);
		}
		super.start();
	}

	/**
	 * Stops the Source's event processing and shuts down the Twitter stream.
	 */
	@Override
	public void stop() {
		logger.debug("Shutting down Twitter sample stream...");
		twitterStream.shutdown();
		super.stop();
	}
	
	private PointDto status2PointDto(Status status){
		
		PointDto result = new PointDto();
		result.setMeasurement(POINT_MESAUREMENT);
		result.addField("text", status.getText());
		if(null != status.getGeoLocation()){
			result.addTag("latitude", Double.toString(status.getGeoLocation().getLatitude()));
			result.addTag("longitude", Double.toString(status.getGeoLocation().getLongitude()));
		}

		if(null !=status.getPlace() && null != status.getPlace().getCountry())
			result.addTag("country", status.getPlace().getCountry());
		
		result.setTimestamp(status.getCreatedAt().getTime());

		return result;
		
	}
	
}
