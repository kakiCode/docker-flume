#!/bin/bash

CONF_DIR=${CONF_DIR:-/opt/flume/conf}

[[ -z "${CONF_FILE}"  ]] && { echo "CONF_FILE required";  exit 1; }
[[ -z "${AGENT_NAME}" ]] && { echo "AGENT_NAME required"; exit 1; }
#[[ -z "${KEYWORDS}" ]] && { echo "KEYWORDS required"; exit 1; }
#[[ -z "${TICKERS}" ]] && { echo "TICKERS required"; exit 1; }

echo "Starting flume agent : ${AGENT_NAME}"
# echo "keywords: ${KEYWORDS}"
# export KEYWORDS="${KEYWORDS}"
# export TICKERS="${TICKERS}"

debug_switch=""
if [ ! -z "$DEBUG_PORT" ]; then
	debug_switch="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
fi

flume-ng agent -c ${CONF_DIR} -f ${CONF_FILE} -n ${AGENT_NAME} -Dflume.root.logger=DEBUG,console -DXms1024m -DXmx4096m $debug_switch

