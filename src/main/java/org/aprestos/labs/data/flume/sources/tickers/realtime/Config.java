package org.aprestos.labs.data.flume.sources.tickers.realtime;

public interface Config {
	
	static final String 
		API_KEY = "apikey" 
		, TICKERS = "tickers"
		, INTERVAL = "interval"
		, DELAY_IN_MILLIS = "delayInMillis" 
		
		, QUERY_PARAM_FUNCTION = "function"
		, QUERY_PARAM_INTERVAL = "interval"
		, QUERY_PARAM_SYMBOL = "symbol"
		, QUERY_PARAM_APIKEY = "apikey"
		, URI = "https://www.alphavantage.co/query"
		, FUNCTION = "TIME_SERIES_INTRADAY"
		, MEASUREMENT_TICKER = "ticker"
		;
	
	static final String DEFAULT_INTERVAL = "5min"; 
	static final int DEFAULT_DELAY =  5 * 60 * 1000;// 5 minutes
	
	//https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=MSFT&interval=1min&apikey=demo
}
