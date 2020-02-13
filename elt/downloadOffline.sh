#!/bin/bash

export TILESERVER="tileserver.leidoslabs.com"
export TGTDIR="offline_cache/$TILESERVER/tileserver"
mkdir -p $TGTDIR

aws s3 sync s3://advanced-analytics-geo-tile-images $TGTDIR
