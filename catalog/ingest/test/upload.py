import os
import json
import boto3
import time

sns_arn = 'arn:aws:sns:us-east-1:555555555555:advanced-analytics-image-metadata'

client = boto3.client('sns')

# Standalone SNS Publisher for when you already have metadata downloaded
path = './metadata'
for file in os.listdir(path):
    if(file.endswith('.json')):
        print('Uploading ' + file)
        json_file = open(os.path.join(path,file), 'r')
        msg = json_file.read()
        response = client.publish(
            TargetArn = sns_arn,
            Message = msg
        )
        print(response)
        json_file.close()
        time.sleep(1)
