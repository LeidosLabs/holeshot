#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/lsImages $1 |
sed 's#"\([^/]*\)/.*",#\1#' |
while read IMAGE
do
   TILESERVER="tileserver.leidoslabs.com"
   TGTDIR="offline_cache/$TILESERVER/tileserver/$IMAGE"
#   mkdir -p $TGTDIR

   echo aws s3 sync s3://advanced-analytics-geo-tile-images/$IMAGE $TGTDIR
done
