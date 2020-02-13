#/bin/bash

export SCRIPT_DIR="$(dirname $0)"
export TILESERVER_STACK="$1"
export MAX_WEB_SERVERS="$2"
export MAX_AGENTS="$3"
export DESIRED_LOAD="$4"
export TILESERVER_INSTANCETYPE="$5"

export TILESERVER_STACKNAME="image-tile-service-$1"


$SCRIPT_DIR/../../main/cloudformation/cloudform.sh $TILESERVER_STACK $MAX_WEB_SERVERS $TILESERVER_INSTANCETYPE
aws cloudformation wait stack-create-complete --stack-name $TILESERVER_STACKNAME
   
export GRINDER_STACKNAME="grinder-tile-server-test-$USERNAME-$TILESERVER_STACK"
$SCRIPT_DIR/grinderCF.sh $DESIRED_LOAD $MAX_AGENTS $TILESERVER_STACK
aws cloudformation wait stack-create-complete --stack-name $GRINDER_STACKNAME 
