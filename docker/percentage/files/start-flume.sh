#!/bin/bash

CONF_DIR=/opt/flume/conf
CONF_FILE=flume.conf
AGENT_NAME=kaki-agent

echo "Starting flume agent : ${AGENT_NAME}"

debug_switch=""
if [ ! -z "$DEBUG_PORT" ]; then
	debug_switch="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
fi

flume-ng agent -c ${CONF_DIR} -f ${CONF_FILE} -n ${AGENT_NAME} -Dflume.root.logger=DEBUG,console -DXms1024m -DXmx4096m $debug_switch

