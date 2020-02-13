#/bin/bash

#function getWebServerPlacementGroup() {
#	 aws --output json cloudformation describe-stack-resources \
#	    --stack-name "$1" --logical-resource-id WebServerPlacementGroup \
#	    |sed -n '/PhysicalResourceId/s/^.*: "\([^"]*\).*$/\1/p'
#}

export TILESERVER_STACK="$3"
export TILESERVER_STACKNAME="image-tile-service-$TILESERVER_STACK"
export STACK_NAME="grinder-tile-server-test-$USERNAME-$TILESERVER_STACK"
export CURRENT_DIR="$(pwd)"
export SCRIPT_DIR="$(dirname $0)"
export TEMPLATE_BUCKET="leidoslabs/cf-templates/image-tile-service-$USERNAME"
export TEMPLATE_URL="s3://$TEMPLATE_BUCKET"
export REGION=us-east-1
export KEY_NAME=advanced-analytics-05-18-2017
export DESIRED_LOAD="$1"
export MAX_AGENTS="$2"
export TILESERVER_URL="https://$USERNAME-$TILESERVER_STACK-tileserver.leidoslabs.com/tileserver"
export TILESERVER_USER="$4"
export TILESERVER_PASSWORD="$5"
export TILEBUCKET="advanced-analytics-geo-tile-images"

export TEST_PROPERTIES=grinder-tile-server.properties
export TEST_DIRECTORY=$TEMPLATE_URL/grinderscripts
#export WEB_SERVER_PLACEMENT_GROUP="$(getWebServerPlacementGroup $TILESERVER_STACKNAME)"

sed "s#{{TILESERVER_URL}}#$TILESERVER_URL#g;s#{{TILESERVER_USER}}#$TILESERVER_USER#g;s#{{TILESERVER_PASSWORD}}#$TILESERVER_PASSWORD#g" $SCRIPT_DIR/httpget.py.tpl > $SCRIPT_DIR/httpget.py
aws s3 ls --recursive $TILEBUCKET | grep '\.png$' > $SCRIPT_DIR/testImages
aws s3 sync $SCRIPT_DIR $TEMPLATE_URL/grinderscripts
aws s3 cp grinder-load-testing.template $TEMPLATE_URL/grinder-load-testing.template

aws cloudformation create-stack --stack-name $STACK_NAME \
    --template-url https://s3.amazonaws.com/$TEMPLATE_BUCKET/grinder-load-testing.template \
    --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
    --disable-rollback \
    --region $REGION \
    --parameters ParameterKey=DesiredLoad,ParameterValue=$DESIRED_LOAD \
                 ParameterKey=DesiredIncreasePercentage,ParameterValue=50 \
                 ParameterKey=DesiredIntervalBetweenIncreases,ParameterValue=360 \
                 ParameterKey=KeyPairName,ParameterValue=$KEY_NAME \
                 ParameterKey=MaxAgents,ParameterValue=$MAX_AGENTS \
                 ParameterKey=TestProperties,ParameterValue=$TEST_PROPERTIES \
                 ParameterKey=TestDirectory,ParameterValue=$TEST_DIRECTORY
                 

rm $SCRIPT_DIR/testImages
rm $SCRIPT_DIR/httpget.py
   
cd $CURRENT_DIR
