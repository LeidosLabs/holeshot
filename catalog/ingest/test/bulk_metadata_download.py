import boto3
prefixes =  \
['3v040416p0000341761a520000100232m_001651391_1GST/',
 '3v051207p0001002641a520004800722m_001627009_1GST/']

# Download metadata for a subset of images selected by some other means

s3_client = boto3.client('s3')
s3_resource = boto3.resource('s3')
bucket = 'advanced-analytics-geo-tile-images'
staging_path = './metadata/'

for prefix in prefixes:
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