#/bin/bash

function waitForKill() {
   killInput=""
   while [[ "$killInput" != "kill" ]]
   do
      echo -e "\nEnter 'kill' to delete stack and move to next test\n"
      read -p killInput
   done
}

function getWebServerASGName() {
	 aws --output json cloudformation describe-stack-resources \
	    --stack-name "$1" --logical-resource-id WebServerASG \
	    |sed -n '/PhysicalResourceId/s/^.*: "\([^"]*\).*$/\1/p'
}

function getCloudWatchFilters() {
   export ASG_NAME="$(getWebServerASGName $1)"
   
   cwFilter="(~'GRINDER~'TPS~'Stack~'$2~(period~300))"
   cwFilter+="~(~'.~'TotalMeanTestTimeMs~'.~'.~(period~300))"
   cwFilter+="~(~'.~'TotalErrors~'.~'.~(period~300))"
   cwFilter+="~(~'AWS*2fEC2~'CPUUtilization~'AutoScalingGroupName~'$ASG_NAME~(period~300))"
   cwFilter+="~(~'.~'NetworkOut~'.~'.~(period~300))"
   
   echo "$cwFilter"
}

function getCloudWatchURL() {
   cwURL="https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#metricsV2:graph=~(view~'timeSeries~stacked~false~region~'us-east-1~metrics~(~"
   cwURL+="$1"
   cwURL+=")~start~'-PT3H~end~'P0D);namespace=GRINDER;dimensions=Stack"
   
   echo "$cwURL"
}

export SCRIPT_DIR="$(dirname $0)"
fullCWFilter=""
while read instanceType stack
do
   TILESERVER_STACKNAME="image-tile-service-$stack"
   GRINDER_STACKNAME="grinder-tile-server-test-$USERNAME-$stack"

   echo "Launching $instanceType $stack"
   $SCRIPT_DIR/fullTest.sh $stack 10 10 3000 $instanceType

   if [ -n "$fullCWFilter" ]; then
     fullCWFilter+="~"
   fi
   fullCWFilter+="$(getCloudWatchFilters $TILESERVER_STACKNAME $GRINDER_STACKNAME)"
   fullCWUrl="$(getCloudWatchURL $fullCWFilter)"
   
   echo "Cloudwatch URL = $fullCWUrl"
   echo "Testing $instanceType $stack"
   
   # waitForKill
   sleep 45m
   
   echo "killing $instanceType $stack"
   aws cloudformation delete-stack --stack-name "$GRINDER_STACKNAME"
   aws cloudformation delete-stack --stack-name "$TILESERVER_STACKNAME"
   
   
done <<EOF
c5.large 91
EOF

#c5.large 91
#c5.xlarge 92
#c5.2xlarge 93
#c5.4xlarge 95
#c5.9xlarge 96
#c5.18xlarge 97
