#! /bin/bash

JENKINS_URL=https://jenkins.edgexfoundry.org
JENKINS_CRUMB=`curl --silent "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)"`
JENKINS_VAL="$JENKINS_URL/pipeline-model-converter/validate"
JENKINS_FILE_LIST=(`grep -l "pipeline\s*{" vars/*`)

for JENKINS_FILE in "${JENKINS_FILE_LIST[@]}"
do
	ret=$(curl --silent -X POST -H $JENKINS_CRUMB -F "jenkinsfile=<$JENKINS_FILE" $JENKINS_VAL)
	if [[ $ret == *"Errors"* ]];then
		echo $ret
		echo "Linting has failed for $JENKINS_FILE"
		exit 1
	else
		echo "$JENKINS_FILE successfully validated"
	fi
done
