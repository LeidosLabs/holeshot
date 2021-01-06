#!/bin/bash

export STACK_SUFFIX=$(echo "${1:-dev}" | tr "[:upper:]" "[:lower:]")
export PRIMARY_STACK="${2:-false}"

if [ $PRIMARY_STACK = true ]
then
    STACK_SUFFIX=prod
fi

echo "starting up tile-service on $(date)"
/bin/bash $(pwd)/tile-service/src/main/cloudformation/cloudform.sh $STACK_SUFFIX $PRIMARY_STACK
echo "completed tile-service on $(date)"

echo "starting up chipper-service on $(date)"
/bin/bash $(pwd)/chipper-service/src/main/cloudformation/cloudform.sh $STACK_SUFFIX $PRIMARY_STACK
echo "completed chipper-service on $(date)"

echo "starting up ingest-service on $(date)"
/bin/bash $(pwd)/ingest-service/src/main/resources/cloudform.sh $STACK_SUFFIX $PRIMARY_STACK
echo "completed ingest-service on $(date)"

echo "starting catalog deploy on $(date)"
echo "Awaiting tile-service stack creation"
/bin/bash $(pwd)/catalog/scripts/deployCatalog.sh $STACK_SUFFIX
echo "finished catalog deploy on $(date)"