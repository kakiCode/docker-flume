package org.aprestos.labs.data.flume.sources.tickers.realtime;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TickersTest {

	@Test
	public void test() throws KeyManagementException, NoSuchAlgorithmException {
		
		Map<String, String> result = new HashMap<String, String>();
		result.put(Config.QUERY_PARAM_APIKEY, "RFD7RYO099TL9TYB");
		result.put(Config.QUERY_PARAM_FUNCTION, Config.FUNCTION);
		result.put(Config.QUERY_PARAM_INTERVAL, "1min");
		
		new Tickers.Runner(result, "MSFT", null).run();
		
	}

}
