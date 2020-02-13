

function packageIngestLambda() {
    rm ../cloudformation/catalog/image-catalog-ingest.zip
    mkdir tmp
    cp ../ingest/lambda/* tmp
    pip install -r tmp/requirements.txt -t tmp
    # Create zip file, will need to be changed if you run on mac/linux
    C:/'Program Files'/7-Zip/7z.exe a ../cloudformation/catalog/image-catalog-ingest.zip ./tmp/*
    rm -rf tmp
    #cd ../ingest/lambda
    #zip ../../cloudformation/image-catalog-ingest.zip -R ./*
    #cd ../../cloudformation
}

function packageAPI() {
    cd ../api
    chalice package "staging/"

    aws cloudformation package \
         --template-file "staging/sam.json" \
         --s3-bucket $S3_BUCKET \
         --s3-prefix "api" \
         --region $REGION \
         --output-template-file "staging/catalog-api.template" \
         --use-json

    # Hack to parameterize the generated template. Watch https://github.com/aws/chalice/issues/608
    sed -i '/"Resources"/i \
	"Parameters": { \
	    "ElasticsearchEndpoint": { \
		    "Type": "String" \
		}, \
		"UserPoolARN": { \
		    "Type": "String" \
		} \
	},' "staging/catalog-api.template"
    
    sed -i 's/"ELASTICSEARCHENDPOINT"/{"Ref": "ElasticsearchEndpoint"}/g' "staging/catalog-api.template"
    sed -i 's/"USERPOOLARN"/{"Ref": "UserPoolARN"}/g' "staging/catalog-api.template"
    
    aws s3 cp "staging/catalog-api.template" "s3://$S3_BUCKET/api/catalog-api.template" --region $REGION

    cd ../scripts
}

function cfImageCatalog() {
    
    packageIngestLambda
    packageAPI
    aws s3 sync --region $REGION ../cloudformation/catalog "s3://$S3_BUCKET/catalog"

    export STACK_NAME="image-catalog-$NOW"
    aws cloudformation create-stack --stack-name $STACK_NAME \
       --template-url "$S3_URL/catalog/image-catalog.template" \
       --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
       --region $REGION \
       --parameters ParameterKey=NameSuffix,ParameterValue="ic$NOW" \
                    ParameterKey=SecurityGroups,ParameterValue=$SECURITY_GROUPS \
                    ParameterKey=Subnet,ParameterValue=$SUBNET \
                    ParameterKey=ESVersion,ParameterValue=$ES_VERSION \
                    ParameterKey=ESNodeVolumeSize,ParameterValue=$ES_DATA_NODE_VOLUME \
                    ParameterKey=ESMasterNodeCount,ParameterValue=$ES_MASTER_NODE_COUNT \
                    ParameterKey=ESMasterInstanceType,ParameterValue=$ES_MASTER_INSTANCE_TYPE \
                    ParameterKey=ESDataNodeCount,ParameterValue=$ES_DATA_NODE_COUNT \
                    ParameterKey=ESDataInstanceType,ParameterValue=$ES_DATA_INSTANCE_TYPE \
                    ParameterKey=ImageMetadataSNSTopic,ParameterValue=$METADATA_TOPIC \
                    ParameterKey=TileserverURL,ParameterValue=$TILESERVER_URL \
                    ParameterKey=S3DeploymentBucket,ParameterValue=$S3_BUCKET \
                    ParameterKey=TemplateBaseURL,ParameterValue=$S3_URL \
                    ParameterKey=UserPoolARN,ParameterValue=$USER_POOL_ARN \
                    ParameterKey=S3BackupBucket,ParameterValue=$S3_BACKUP_BUCKET

}

function cfElasticSearchOnly() {
    
    aws s3 sync --region $REGION ../cloudformation/catalog "s3://$S3_BUCKET/catalog"

    export STACK_NAME="image-catalog-es-$NOW"
    aws cloudformation create-stack --stack-name $STACK_NAME \
       --template-url "$S3_URL/catalog/elasticsearch.template" \
       --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
       --region $REGION \
       --parameters ParameterKey=NameSuffix,ParameterValue="ic$NOW" \
                    ParameterKey=SecurityGroups,ParameterValue=$SECURITY_GROUPS \
                    ParameterKey=Subnet,ParameterValue=$SUBNET \
                    ParameterKey=ElasticsearchVersion,ParameterValue=$ES_VERSION \
                    ParameterKey=NodeVolumeSize,ParameterValue=$ES_DATA_NODE_VOLUME \
                    ParameterKey=MasterNodeCount,ParameterValue=$ES_MASTER_NODE_COUNT \
                    ParameterKey=MasterInstanceType,ParameterValue=$ES_MASTER_INSTANCE_TYPE \
                    ParameterKey=DataNodeCount,ParameterValue=$ES_DATA_NODE_COUNT \
                    ParameterKey=DataInstanceType,ParameterValue=$ES_DATA_INSTANCE_TYPE
}

function cfIngestLambdaOnly() {
    
    packageIngestLambda
    aws s3 sync --region $REGION ../cloudformation/catalog "s3://$S3_BUCKET/catalog"

    export STACK_NAME="image-catalog-lambda-$NOW"
    aws cloudformation create-stack --stack-name $STACK_NAME \
       --template-url "$S3_URL/catalog/ingest-lambda.template" \
       --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
       --region $REGION \
        --parameters ParameterKey=NameSuffix,ParameterValue="ic$NOW" \
                     ParameterKey=SecurityGroups,ParameterValue=$SECURITY_GROUPS \
                     ParameterKey=Subnet,ParameterValue=$SUBNET \
                     ParameterKey=ImageMetadataSNSTopic,ParameterValue=$METADATA_TOPIC \
                     ParameterKey=TileserverURL,ParameterValue=$TILESERVER_URL \
                     ParameterKey=S3DeploymentBucket,ParameterValue=$S3_BUCKET \
                     ParameterKey=ElasticsearchEndpoint,ParameterValue=$ES_ENDPOINT \
                     ParameterKey=S3BackupBucket,ParameterValue=$S3_BACKUP_BUCKET
}

function cfAPIOnly() {

    packageAPI

    export STACK_NAME="image-catalog-api-$NOW"
    aws cloudformation create-stack --stack-name $STACK_NAME \
        --template-url "$S3_URL/api/catalog-api.template" \
        --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM  CAPABILITY_AUTO_EXPAND \
        --region $REGION \
        --parameters ParameterKey=UserPoolARN,ParameterValue=$USER_POOL_ARN \
                     ParameterKey=ElasticsearchEndpoint,ParameterValue=$ES_ENDPOINT
        

}

export NOW="$(date +%s)"
export REGION="us-east-1"
export VPC="vpc-29d17050"
export METADATA_TOPIC="arn:aws:sns:us-east-1:199974664221:advanced-analytics-image-metadata"
export TILESERVER_URL="https://tileserver.leidoslabs.com/tileserver"
export S3_BUCKET="advanced-analytics-image-catalog"
export S3_URL="https://s3.$REGION.amazonaws.com/$S3_BUCKET"
export ES_VERSION="6.4"
export SUBNET='subnet-58fbd464'
export ES_DATA_NODE_COUNT=1
export ES_DATA_INSTANCE_TYPE="t2.small.elasticsearch"
export ES_DATA_NODE_VOLUME=10
export ES_MASTER_NODE_COUNT=0
export ES_MASTER_INSTANCE_TYPE="t2.small.elasticsearch"
export SECURITY_GROUPS="sg-e157bb90"
export USER_POOL_ARN="arn:aws:cognito-idp:us-east-1:199974664221:userpool/us-east-1_b3xFBA6h1"
export S3_BACKUP_BUCKET="advanced-analytics-image-metadata-backup"
export ES_ENDPOINT="vpc-adv-analytics-es-small-auhfhkgcnz3oqxfx66vfxfddwq.us-east-1.es.amazonaws.com"

#To deploy the entire API (You will need to manually upload the elasticsearch mapping)
#cfImageCatalog


export SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export CUR_DIR="$( pwd )"
cd $SCRIPT_DIR

#Deploy individual components
#cfElasticSearchOnly
#cfIngestLambdaOnly
cfAPIOnly

cd $CUR_DIR