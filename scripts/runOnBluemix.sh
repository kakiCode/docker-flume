#!/bin/sh

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)

. $scripts_folder/VARS.sh

CONTAINER_ID=`cf ic ps -a | grep -E "$BLUEMIX_IMG:$IMG_VERSION" | awk -e '{print $1}'`
echo "...container id: $CONTAINER_ID"

if [ ! -z "$CONTAINER_ID" ]; then
	
	cf ic stop $CONTAINER_ID

	stopped="1"
	while [ "$stopped" -ne "0" ]
	do
	        cf ic ps -a | grep $CONTAINER_ID | grep Shutdown
	        stopped=$?
	        echo "has container stopped (0=true) ? $stopped"
	        sleep 6
	done

	cf ic rm $CONTAINER_ID

	removed="0"
	while [ "$removed" -ne "1" ]
	do
	        cf ic ps -a | grep $CONTAINER_ID
	        removed=$?
	        echo "has container been removed (1=true) ? $removed"
	        sleep 6
	done

fi

cf ic run --name $CONTAINER --link $ZK_CONTAINER:$ZK_HOST --link $KAFKA_CONTAINER:$KAFKA_HOST --env AGENT_NAME=$AGENT -m $BX_CONTAINER_MEMORY $BLUEMIX_IMG

running="1"
while [ "$running" -ne "0" ]
do
        cf ic ps -a | grep $CONTAINER | grep Running
        running=$?
        echo "is container running (0=true) ? $running"
        sleep 6
done

sleep 12
cf ic logs $CONTAINER