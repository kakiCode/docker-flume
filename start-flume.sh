#!/bin/bash

CONF_DIR=${CONF_DIR:-/opt/flume/conf}

[[ -z "${CONF_FILE}"  ]] && { echo "CONF_FILE required";  exit 1; }
[[ -z "${AGENT_NAME}" ]] && { echo "AGENT_NAME required"; exit 1; }

echo "Starting flume agent : ${AGENT_NAME}"

flume-ng agent \
  -c ${CONF_DIR} \
  -f ${CONF_FILE} \
  -n ${AGENT_NAME} \
  -Dflume.root.logger=INFO,console

