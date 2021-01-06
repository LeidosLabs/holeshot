#!/bin/bash

#usage: cloudform.sh "stack-suffix=#" "primary-stack=true|false" "max-#-tile-servers=#"
export CURRENT_DIR="$(pwd)"
export SCRIPT_DIR="$(dirname $0)"
export TEMPLATE_BUCKET="leidoslabs/cf-templates/image-tile-service-$USERNAME"
export TEMPLATE_URL="s3://$TEMPLATE_BUCKET"
export REGION=us-east-1
export KEY_NAME=advanced-analytics-05-18-2017
export TILE_IMAGE_BUCKET=advanced-analytics-geo-tile-images
export STACK_SUFFIX="${1:-dev}"
export PRIMARY_STACK="${2:-false}"
export MAX_TILE_SERVERS="${3:-5}"

if [ $PRIMARY_STACK = true ]
then
    STACK_SUFFIX=prod
fi

export STACK_NAME=tileserver-$STACK_SUFFIX

# Set stack dependent parameters (topics, bucket names)
if [ $PRIMARY_STACK = true ]
then
export TileImagesBucket="$TILE_IMAGE_BUCKET"
else
export TileImagesBucket="$TILE_IMAGE_BUCKET-$STACK_SUFFIX"
fi

# $SCRIPT_DIR/../../../buildAll.sh clean install "-Dmaven.test.skip=true"

aws s3 sync $SCRIPT_DIR $TEMPLATE_URL
aws s3 sync --exclude "*" --include tileserver.jar $SCRIPT_DIR/../../../target $TEMPLATE_URL 
aws s3 sync --exclude "*" --include log4j.properties $SCRIPT_DIR/../../../src/main/resources $TEMPLATE_URL


aws cloudformation create-stack --stack-name $STACK_NAME \
    --template-url https://s3.amazonaws.com/$TEMPLATE_BUCKET/tileserver.template \
    --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
    --disable-rollback \
    --region $REGION \
    --parameters ParameterKey=DeploymentArtifactsBucketName,ParameterValue=$TEMPLATE_BUCKET \
                 ParameterKey=KeyName,ParameterValue=$KEY_NAME \
                 ParameterKey=StackSuffix,ParameterValue=$STACK_SUFFIX \
                 ParameterKey=PrimaryStack,ParameterValue=$PRIMARY_STACK \
                 ParameterKey=TileBucketName,ParameterValue=$TileImagesBucket \
                 ParameterKey=MaxTileservers,ParameterValue=$MAX_TILE_SERVERS

cd $CURRENT_DIR