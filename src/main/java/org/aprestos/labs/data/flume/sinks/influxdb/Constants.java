package org.aprestos.labs.data.flume.sinks.influxdb;

public interface Constants {
	static final String 
		DB_HOST = "dbHost" 
		, DB_PORT = "dbPort"
		, DB_USER = "dbUser"
		, DB_PSWD = "dbPswd"
		, DB_NAME = "dbName"
		, BATCH_SIZE = "batchSize";
	
	static final int DEFAULT_BATCH_SIZE = 32;
}
