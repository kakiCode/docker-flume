package org.aprestos.labs.data.flume.sources.twitter;

public interface Config {
	
	static final String CONSUMER_KEY_KEY = "consumerKey";
	static final String CONSUMER_SECRET_KEY = "consumerSecret";
	static final String ACCESS_TOKEN_KEY = "accessToken";
	static final String ACCESS_TOKEN_SECRET_KEY = "accessTokenSecret";
	static final String BATCH_SIZE_KEY = "batchSize";
	static final long DEFAULT_BATCH_SIZE = 1000L;
	static final String KEYWORDS_KEY = "keywords";
	static final String POINT_MESAUREMENT = "tweet";
	static final String ENV_VAR_KEYWORDS = "KEYWORDS";
	
}
