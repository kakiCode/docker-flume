#!/bin/sh

scripts_folder=$(dirname $(readlink -f $0))
base_folder=$(dirname $scripts_folder)

. $scripts_folder/VARS.sh

cf ic exec -it $CONTAINER bash

