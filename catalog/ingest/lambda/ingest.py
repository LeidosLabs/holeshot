import boto3
from aws_requests_auth.aws_auth import AWSRequestsAuth
from elasticsearch import Elasticsearch, RequestsHttpConnection
import json
import os
import time

def handle_metadata(event, context):

    #es_endpoint = 'vpc-adv-analytics-es-small-auhfhkgcnz3oqxfx66vfxfddwq.us-east-1.es.amazonaws.com'
    es_endpoint = os.environ['es_endpoint']
    #tileserver = 'https://rgrobert-88-tileserver.leidoslabs.com/tileserver'
    tileserver_url = os.environ['tileserver_url']
    #Backup SNS messages in s3
    s3_backup_bucket = os.environ['s3_backup_bucket']

    session = boto3.session.Session()
    credentials = session.get_credentials().get_frozen_credentials()

    #s3 for backups
    s3 = session.resource('s3')

    # boto3 doesn't support signed requests to ES yet https://github.com/boto/boto3/issues/853
    awsauth = AWSRequestsAuth(
        aws_access_key=credentials.access_key,
        aws_secret_access_key=credentials.secret_key,
        aws_token=credentials.token,
        aws_host=es_endpoint,
        aws_region=session.region_name,
        aws_service='es'
    )

    es_client = Elasticsearch(
        hosts=[{'host': es_endpoint, 'port': 443}],
        http_auth=awsauth,
        use_ssl=True,
        verify_certs=True,
        connection_class=RequestsHttpConnection
    )

    message = json.loads(event["Records"][0]["Sns"]["Message"])



    nitf_metadata = message.pop("metadata")
    nitf_metadata.pop("RPC00B", None) #dump RPCs

    # Manually handle required fields, so we fail if they aren't present
    # Other fields come from inside the 'metadata' tag and are standard NITF fields, with the exception of
    # ignoring the RPCs

    output = {}

    output['edhIdentifier'] = message["edhIdentifier"]

    output['name'] = message["name"]

    output['bounds'] = message['bounds']

    output['minRLevel'] = message['minrlevel']

    output['maxRLevel'] = message['maxRLevel']

    timestamp = str(nitf_metadata["IDATIM"]) #ingest expects yyyyMMddHHmmss, elasticsearch expects yyyy-MM-dd'T'HH:mm:ss.SSSZZ
    output['date'] = timestamp[0:4] + '-' + timestamp[4:6] + '-' + timestamp[6:8] + 'T'  \
                     + timestamp[8:10]  + ':' + timestamp[10:12] + ':' + timestamp[12:14] \
                     + '.000Z'

    name_url = message["name"].replace(':','/')
            
    output['imageLink'] = tileserver_url + '/' + name_url

    output['thumbnailLink'] = output['imageLink'] +  '/' + str(message['maxRLevel']) + '/0/0/0.png'

    for field in nitf_metadata:
        output[field] = nitf_metadata[field]

    es_client.index(index='imagery', doc_type='metadata', body=output, id=message["name"])

    #log to s3 at the end, only want to backup events that were actually ingested sucessfully. Failed messages will be sent through dead letter queue
    s3.Object(s3_backup_bucket, message['name'] + '-' + time.strftime("%Y-%m-%dT%X") + '.json').put(Body=json.dumps(event["Records"][0]["Sns"]["Message"]));
