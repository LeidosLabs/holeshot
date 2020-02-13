import boto3
import os
import json
import time

# This script will download all the metadata.json files associated with imagery in the given bucket, and then submit them to the
# SNS queue as if they had been pushed by the image server. For more than a few thousand images it would probably be wise
# to perform a more direct ingest without the round trip of downloading and republishing the files.

bucket = 'advanced-analytics-geo-tile-images'
sns_arn = 'arn:aws:sns:us-east-1:199974664221:advanced-analytics-image-metadata'

s3_client = boto3.client('s3')
s3_resource = boto3.resource('s3')
sns_client = boto3.client('sns')

keys_per_fetch = 11
max_iter = 1
staging_path = './metadata/'
publish_delay = 1 # seconds to wait between each publish. necessary until the ingest lambda handles backoff retry better

i = 1
total_fetched = 0
is_truncated = True
image_prefixes = []
# Retrieve object names so we can download the metadata files
print('Beginning fetch of up to ' + str(max_iter * keys_per_fetch) + ' image keys')
while is_truncated and i <= max_iter:
    print('Iteration ' + str(i) + '. Attempting to fetch keys ' + str(total_fetched + 1) + '-' + str(total_fetched + keys_per_fetch))
    if i == 1:
        image_keys = s3_client.list_objects_v2(Bucket=bucket, Delimiter='/', MaxKeys=keys_per_fetch)
    else:
        image_keys = s3_client.list_objects_v2(Bucket=bucket, Delimiter='/', MaxKeys=keys_per_fetch, ContinuationToken=continue_token)
    for prefix in image_keys['CommonPrefixes']:
        image_prefixes.append(prefix['Prefix'])
    is_truncated = image_keys['IsTruncated']
    total_fetched += image_keys['KeyCount']
    if is_truncated:
        continue_token = image_keys['NextContinuationToken']
    i = i+1

print('Fetching complete. Total objects: ' + str(total_fetched))

# Download metadata
for prefix in image_prefixes:
    result = s3_client.list_objects_v2(Bucket=bucket, Prefix=prefix, Delimiter='/')
    i=0
    for o in result['CommonPrefixes']:
        local_path = staging_path + prefix[:-1]
        if(len(result['CommonPrefixes']) > 1):
            i = i+1
            local_path = local_path + "_" + str(i)
        s3_path = o['Prefix'] + 'metadata.json'
        print('Getting ' + s3_path)
        s3_resource.Bucket(bucket).download_file(s3_path,  local_path + '.json')

# Publish metadata to SNS
for file in os.listdir(staging_path):
    if(file.endswith('.json')):
        print('Uploading ' + file)
        json_file = open(os.path.join(staging_path, file), 'r')
        msg = json_file.read()
        response = sns_client.publish(
            TargetArn = sns_arn,
            Message = msg
        )
        print(response)
        json_file.close()
        time.sleep(publish_delay)