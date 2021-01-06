#!/usr/bin/env python

import sys
import os
import subprocess
import json
import datetime
import time

TOPIC = 'arn:aws:sns:us-east-1:555555555555:advanced-analytics-image-ingest'
SAMPLE_FILENAME = 'sample-ingest-msg.json'

def main():

    if not os.path.isfile(SAMPLE_FILENAME):
        print("File path {} does not exist. Exiting...".format(SAMPLE_FILENAME))
        sys.exit()
    
    sample_msg = ''
    with open(SAMPLE_FILENAME, 'r') as fp:
        sample_msg = json.load(fp)

    if len(sys.argv) != 2:
        print("Usage: {} <input file>. Exiting...".format(sys.argv[0]))
        sys.exit()

    filepath = sys.argv[1]

    if not os.path.isfile(filepath):
       print("File path {} does not exist. Exiting...".format(filepath))
       sys.exit()
  
    with open(filepath, 'r') as fp:
        lines = (line.rstrip() for line in fp)
        lines = (line for line in lines if line)
        for line in lines:
            msg = make_msg(sample=sample_msg, key=line)
            print("Sending message for " + line, flush=True )
            send_msg(msg)
            time.sleep(1)

def send_msg(msg):
    subprocess.run(['aws', 'sns', 'publish', '--topic-arn', TOPIC, '--message', json.dumps(msg)], stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)

def make_msg(sample, key):
    sample['Records'][0]['s3']['object']['key'] = key
    sample['Records'][0]['eventTime'] = datetime.datetime.utcnow().isoformat() + 'Z'
    return sample

if __name__ == '__main__':
    main()
