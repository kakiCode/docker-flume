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

package org.aprestos.labs.data.flume.sources.twitter;

import java.nio.charset.Charset;
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

public class TwitterSource extends AbstractSource implements EventDrivenSource, Configurable {

	private static final Logger logger = LoggerFactory.getLogger(TwitterSource.class);
	private String[] keywords;

	/** The actual Twitter stream. It's set up to collect raw JSON data */
	private TwitterStream twitterStream;

	/**
	 * The initialization method for the Source. The context contains all the Flume
	 * configuration info, and can be used to retrieve any configuration values
	 * necessary to set up the Source.
	 */
	@Override
	public void configure(Context context) {
		try {
			/** Information necessary for accessing the Twitter API */
			String consumerKey = context.getString(Config.CONSUMER_KEY_KEY);
			String consumerSecret = context.getString(Config.CONSUMER_SECRET_KEY);
			String accessToken = context.getString(Config.ACCESS_TOKEN_KEY);
			String accessTokenSecret = context.getString(Config.ACCESS_TOKEN_SECRET_KEY);

			String keywordString = System.getenv(Config.ENV_VAR_KEYWORDS);
			if (null == keywordString || keywordString.isEmpty() || 0 == keywordString.trim().length())
				throw new IllegalArgumentException(
						String.format("!!! must provide % environment variable !!!", Config.ENV_VAR_KEYWORDS));

			keywords = keywordString.split(",");
			for (int i = 0; i < keywords.length; i++) {
				keywords[i] = keywords[i].trim();
			}

			if (0 == keywords.length)
				throw new IllegalArgumentException(String.format(
						"!!! must provide comma delimited % environment variable !!!", Config.ENV_VAR_KEYWORDS));

			logger.debug(
					"Setting up Twitter filtered stream using consumer key {} and"
							+ " access token {}, filtered on keywords {}",
					new String[] { consumerKey, accessToken, keywordString });

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

		final ChannelProcessor channel = getChannelProcessor();
		final Map<String, String> headers = new HashMap<String, String>();

		StatusListener listener = new StatusListener() {
			// The onStatus method is executed every time a new tweet comes in.
			public void onStatus(Status status) {
				// The EventBuilder is used to build an event using the headers
				// and the raw JSON of a tweet
				try {
					String tag = null;
					for (String keyword : keywords)
						if (status.getText().toLowerCase().contains(keyword.toLowerCase())) {
							tag = keyword;
							break;
						}

					if (null == tag) {
						logger.debug("no keyword in text");
						return;
					}

					logger.debug(status.getUser().getScreenName() + ": " + status.getText());
					headers.put("timestamp", String.valueOf(status.getCreatedAt().getTime()));
					PointDto point = status2PointDto(status);
					point.addTag("keyword", tag);
					logger.info("processed tweet: {}", point.toString());

					Event event = EventBuilder.withBody(PointUtils.toBytes(point), headers);
					channel.processEvent(event);
				} catch (Exception e) {
					logger.error("onStatus", e);
				}
			}

			// This listener will ignore everything except for new tweets
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				logger.trace("@onDeletionNotice");
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				logger.trace("@onTrackLimitationNotice");
			}

			public void onScrubGeo(long userId, long upToStatusId) {
				logger.trace("@onScrubGeo");
			}

			public void onException(Exception ex) {
				logger.error("@onException", ex);
			}

			public void onStallWarning(StallWarning warning) {
				logger.trace("@onStallWarning");
			}

		};

		// Set up the stream's listener (defined above),
		twitterStream.addListener(listener);

		// Set up a filter to pull out industry-relevant tweets
		logger.debug("Starting up Twitter filtering...");
		FilterQuery query = new FilterQuery();
		query.track(keywords);
		twitterStream.filter(query);

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

	private PointDto status2PointDto(Status status) {

		PointDto result = new PointDto();
		result.setMeasurement(Config.POINT_MESAUREMENT);
		// converting text to ascii
		result.addField("text",
				new String(status.getText().getBytes(Charset.defaultCharset()), Charset.forName("ASCII")));
		if (null != status.getGeoLocation()) {
			result.addTag("latitude", Double.toString(status.getGeoLocation().getLatitude()));
			result.addTag("longitude", Double.toString(status.getGeoLocation().getLongitude()));
		}

		if (null != status.getPlace() && null != status.getPlace().getCountry())
			result.addTag("country", status.getPlace().getCountry());

		result.setTimestamp(status.getCreatedAt().getTime());

		return result;

	}

}
