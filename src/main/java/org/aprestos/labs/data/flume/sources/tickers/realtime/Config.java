package org.aprestos.labs.data.flume.sources.tickers.realtime;

public interface Config {
	
	static final String 
		API_KEY = "apikey" 
		, TICKERS = "tickers"
		, ENV_VAR_TICKERS = "TICKERS"
		, ENV_VAR_QUERY_DELAY = "QUERY_DELAY"
		, ENV_VAR_DATA_INTERVAL = "DATA_INTERVAL"
		, INTERVAL = "interval"
		, DELAY_IN_MILLIS = "delayInMillis" 
		
		, QUERY_PARAM_FUNCTION = "function"
		, QUERY_PARAM_INTERVAL = "interval"
		, QUERY_PARAM_SYMBOL = "symbol"
		, QUERY_PARAM_APIKEY = "apikey"
		, URI = "https://www.alphavantage.co/query"
		, FUNCTION = "TIME_SERIES_INTRADAY"
		, MEASUREMENT_TICKER = "ticker"
		
		, QUANDL_APIKEY = "xQ9Sq7ybYvkuhLnyC1JF"
		;
	
	//https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=MSFT&interval=1min&apikey=demo
}
