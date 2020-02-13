from chalice import Chalice, CognitoUserPoolAuthorizer
from elasticsearch import Elasticsearch, RequestsHttpConnection
from elasticsearch_dsl import Search
from aws_requests_auth.aws_auth import AWSRequestsAuth
from geomet import wkt
import boto3
import os


es_endpoint = os.environ['ES_ENDPOINT']
user_pool_arn= os.environ['USER_POOL_ARN']

app = Chalice(app_name='ImageCatalogAPI')
authorizer = CognitoUserPoolAuthorizer('TestPool', provider_arns=[user_pool_arn])
session = boto3.session.Session()
credentials = session.get_credentials().get_frozen_credentials()

awsauth = AWSRequestsAuth(
    aws_access_key=credentials.access_key,
    aws_secret_access_key=credentials.secret_key,
    aws_token=credentials.token,
    aws_host=es_endpoint,
    aws_region=session.region_name,
    aws_service='es'
)

client = Elasticsearch(
    hosts=[{'host': es_endpoint, 'port': 443}],
    http_auth=awsauth,
    use_ssl=True,
    verify_certs=True,
    connection_class=RequestsHttpConnection
)

@app.route('/search', methods=['GET'], authorizer=authorizer, cors=True)
def catalog_search():

    params = app.current_request.query_params

    s = Search(using=client, index='imagery', doc_type="metadata")

    filter_count = 0

    max_results = 1000

    if("st" in params):
        s = s.filter('range', date={'gte': params["st"]})
        filter_count += 1
    if("et" in params):
        s = s.filter('range', date={'lte': params["et"]})
        filter_count += 1
    if("wkt" in params):
        shape_filter = {"shape": wkt.loads(params["wkt"])} 
        s = s.filter('geo_shape', bounds=shape_filter)
        filter_count += 1
    if("debug" in params):
        return(s.to_dict())
        
    s = s[0:max_results]

    if(filter_count > 0):
        result = s.execute().to_dict()
        return([hit["_source"] for hit in result["hits"]["hits"]])
    else:
        return {'Search Failed':'No search parameters were recognized'}


@app.route('/{collectionId}', methods=['GET'], cors=True, api_key_required=True)
def get_by_id(collectionId):
    s = Search(using=client, index='imagery', doc_type="metadata")
    s = s.query("match", name=collectionId)
    response = s.execute()
    return response.to_dict()

@app.route('/all_imagery', methods=['GET'], cors=True, api_key_required=True)
def get_all():
    # This route is a temporary hack for the WMS service. API Key is not real security, just something to try to prevent random bots from getting a response
    s = Search(using=client, index='imagery', doc_type="metadata")
    total = s.count()
    s = s[0:total]
    response = s.execute()
    return response.to_dict()