@echo off

SET S3_BUCKET=advanced-analytics-deployment
SET S3_PREFIX=sam-deploy/tile-analytics
SET REGION=us-east-1
SET JAR_S3_PATH=s3://%S3_BUCKET%/%S3_PREFIX%/log-mapreduce/
SET JAR_VERSION=0.2

:: SAM won't autodetect changes and upload the EMR jar, so comment/uncomment this line as necessary
:: aws s3 cp "./log-mapreduce/target/log-mapreduce-%JAR_VERSION%.jar" %JAR_S3_PATH%

sam deploy ^
    --stack-name tile-analytics-primary ^
    --region %REGION% ^
    --s3-bucket %S3_BUCKET% ^
    --s3-prefix %S3_PREFIX% ^
    --confirm-changeset ^
    --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND ^
    --parameter-overrides ^
        PrimaryStack="true" ^
        StackSuffix="primary" ^
        AnalyticsApiKeyValue="APIKEYVALUE2" ^
        TileserverUrl="tileserver-dev.leidoslabs.com/tileserver" ^
        TileserverApiKey="APIKEYVALUE" ^
        CatalogUrl="catalog.leidoslabs.com/imagecatalog" ^
        ElasticsearchEndpoint="es-endpoint.us-east-1.es.amazonaws.com" ^
        IngestSNSTopicArn="arn:aws:sns:us-east-1:555555555555:new-image-ingest-complete" ^
        FirehoseLogDeliveryStreamName="advanced-analytics-eltLogging" ^
        Subnet="subnet-08e8b4f8a0a435af0" ^
        WarehouseSecurityGroup="sg-e157bb90" ^
        RolePermissionsBoundary="arn:aws:iam::555555555555:policy/DeveloperPolicy" ^
        ParentDNSName="leidoslabs.com" ^
        HostedZoneCertificateArn="arn:aws:acm:us-east-1:555555555555:certificate/430f2509-82b5-4bcb-bb35-d0fc384df07a" ^
        PublicZoneId="ZZZZZZZZZZZZ" ^
        PrivateZoneId="ZZZZZZZZZZZZzzzzzzzzzz" ^
        IpWhitelist="11.22.33.44.555/32, 11.22.33.44.777/32" ^
        EMRSecurityGroup="sg-03e44e08fc584841c" ^
        InputLogsS3Path="s3://advanced-analytics-tileserver-request-logs" ^
        EMRJobLogsS3Path="s3://advanced-analytics-emr-updated/pipeline-logs" ^
        JarFileS3Path="%JAR_S3_PATH%/log-mapreduce-%JAR_VERSION%.jar" ^
        KeyPair="advanced-analytics-05-18-2017" ^
        CoreInstanceType="m4.large" ^
        CoreInstanceCount="1" ^
        MasterInstanceType="m4.large"