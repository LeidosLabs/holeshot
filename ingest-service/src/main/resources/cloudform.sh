#!/bin/bash

# Run this batch file from this directory to create an ingest-service stack.
# To enable JP2K support, update the $SCRIPT_DIR/ingestserver.config.cfg file

#usage: cloudform.sh "stack-suffix" "enable JP2K=1|0"
export CURRENT_DIR="$(pwd)"
export SCRIPT_DIR="$(dirname $0)"
export TEMPLATE_BUCKET="leidoslabs/cf-templates/ingest-service-$USERNAME"
export TEMPLATE_URL="s3://$TEMPLATE_BUCKET"
export REGION=us-east-1
export KEY_NAME=advanced-analytics-05-18-2017
export SUBNETS="subnet-08e8b4f8a0a435af0\,subnet-02f3aef632fc76739"
export BASTION_CIDR="10.0.0.0/24"
export STACK_SUFFIX="${1:-dev}"
export PRIMARY_STACK="${2:-false}"
if [ $PRIMARY_STACK = true ]
then
    STACK_SUFFIX=prod
fi
export STACK_NAME=ingest-service-$STACK_SUFFIX

# Read configurable values from config file
export configFile="$SCRIPT_DIR/ingestserver.config.cfg"
declare -A config
IFS="="
while read key value; do
    config[$key]="$value"
done < $configFile

export JP2K="${config[JP2K]}"

# Set stack dependent parameters (topics, bucket names)
if [ $PRIMARY_STACK = true ]
then
export TileImagesBucket="${config[TileImagesBucket]}"
export RawImagesBucketName="${config[RawImagesBucket]}"
export ImageMetadataTopicName="${config[ImageMetadataTopic]}"
else
export TileImagesBucket="${config[TileImagesBucket]}-$STACK_SUFFIX"
export RawImagesBucketName="${config[RawImagesBucket]}-$STACK_SUFFIX"
export ImageMetadataTopicName="${config[ImageMetadataTopic]}-$STACK_SUFFIX"
fi
export ImageIngestTopic="${config[ImageIngestTopic]}-$STACK_SUFFIX"


# Check if we need to create s3 buckets
TileBucketStatus=$(aws s3api head-bucket --bucket $TileImagesBucket 2>&1)
if [ -z "$TileBucketStatus" ] ;
then
echo "${TileImagesBucket} exists, stack will use existing bucket"
export CreateTileBucket=false
else
echo "${TileImagesBucket} bucket will be created"
export CreateTileBucket=true
fi

RawBucketStatus=$(aws s3api head-bucket --bucket $RawImagesBucketName 2>&1)
if [ -z "$RawBucketStatus" ] ;
then
echo "${RawImagesBucketName} exists, stack will use existing bucket"
export CreateRawBucket=false
else
echo "${RawImagesBucketName} bucket will be created"
export CreateRawBucket=true
fi

# Check if we need to create the metadata topic
ImageMetadataStatus=$(aws sns list-topics | grep -c $ImageMetadataTopicName || true)
if [ "$ImageMetadataStatus" == "1" ] ;
then 
echo "${ImageMetadataTopicName} exists, stack will use existing topic"
export CreateImageMetadataTopic=false
else 
echo "${ImageMetadataTopicName} topic will be created"
export CreateImageMetadataTopic=true
fi

# $SCRIPT_DIR/../../../buildAll.sh clean install "-Dmaven.test.skip=true"

aws s3 sync $SCRIPT_DIR $TEMPLATE_URL
aws s3 sync --exclude "*" --include ingest-service.jar $SCRIPT_DIR/../../../target $TEMPLATE_URL 
                 
aws cloudformation create-stack --stack-name $STACK_NAME \
    --template-url https://s3.amazonaws.com/$TEMPLATE_BUCKET/ingest.template \
    --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
    --disable-rollback \
    --region $REGION \
    --parameters ParameterKey=DeploymentArtifactsBucketName,ParameterValue=$TEMPLATE_BUCKET \
                 ParameterKey=StackSuffix,ParameterValue=$STACK_SUFFIX \
                 ParameterKey=JP2K,ParameterValue=$JP2K \
                 ParameterKey=TileBucketName,ParameterValue=$TileImagesBucket \
                 ParameterKey=RawImagesBucketName,ParameterValue=$RawImagesBucketName \
                 ParameterKey=MetadataTopicName,ParameterValue=$ImageMetadataTopicName \
                 ParameterKey=NewDataItemSNSTopic,ParameterValue=$ImageIngestTopic \
                 ParameterKey=CreateTileImagesBucket,ParameterValue=$CreateTileBucket \
                 ParameterKey=CreateRawImagesBucket,ParameterValue=$CreateRawBucket \
                 ParameterKey=CreateImageMetadataTopic,ParameterValue=$CreateImageMetadataTopic \
                 ParameterKey=Subnets,ParameterValue=$SUBNETS \
                 ParameterKey=SSHLocation,ParameterValue=$BASTION_CIDR \
                 ParameterKey=KeyName,ParameterValue=$KEY_NAME


cd $CURRENT_DIR
