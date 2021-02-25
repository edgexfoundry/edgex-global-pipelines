#!/bin/bash

set -e -o pipefail

# jenkins will provide these
# JENKINS_URL=https://jenkins.edgexfoundry.org
# BUILD_NUMBER=13
# JOB_NAME=edgexfoundry/sample-service/PR-93

#remove trailing slash
JENKINS_URL_CLEAN=${JENKINS_URL%/}

#build component of build rest url
url_base=${JENKINS_URL_CLEAN}/blue/rest/organizations/jenkins/pipelines

slashes="${JOB_NAME//[^\/]}"
num_slashes="${#slashes}"

if [ $num_slashes -eq 1 ]; then
    org_name=$(echo $JOB_NAME | cut -d'/' -f1)
    project_name=$(echo $JOB_NAME | cut -d'/' -f2)

    node_url="$url_base/$org_name/pipelines/$project_name/runs/$BUILD_NUMBER/nodes/?limit=10000"

elif [ $num_slashes -eq 2 ]; then
    org_name=$(echo $JOB_NAME | cut -d'/' -f1)
    project_name=$(echo $JOB_NAME | cut -d'/' -f2)
    project_branch=$(echo $JOB_NAME | cut -d'/' -f3)

    node_url="$url_base/$org_name/pipelines/$project_name/branches/$project_branch/runs/$BUILD_NUMBER/nodes/?limit=10000"
else
    node_url="$url_base/$JOB_NAME/pipelines/runs/$BUILD_NUMBER/nodes/?limit=10000"
fi

node_json=$(curl -s "$node_url")

failed=$(echo "$node_json" | jq -r '.[] | select(.result == "FAILURE") | .id')
# echo "[debug] Found Failed Nodes:" ; echo "$failed"

# this build will return:
# <last failed stage name>
# ^^^^^^^^^^^^
# <log>

if [ "$failed" != "" ]; then
    # the last failed node should be one we are looking for
    id=$(echo "$failed" | tail -n 1)

    failedNode=$(echo "$node_json" | jq ".[] | select(.id == \"$id\")")
    nodeName=$(echo "$failedNode" | jq -r ".displayName")
    stepsHref=$(echo "$failedNode" | jq -r "._links.steps.href")
    steps_json=$(curl -s "${JENKINS_URL_CLEAN}${stepsHref}")
    failed_steps=$(echo "$steps_json" | jq -r '.[] | select(.result == "FAILURE") | .id')
    # echo "[debug] Found Failed Steps:" ; echo "$failed_steps"

    # only pull log for last failed step
    if [ "$failed_steps" != "" ]; then
        failed_step_id=$(echo $failed_steps | tail -n 1)
        last_failed=$(echo "$steps_json" | jq -r ".[] | select(.id == \"$failed_step_id\")")
        logHref=$(echo "$last_failed" | jq -r ".actions[] | select(.urlName == \"log\")._links.self.href")
        if [ "$logHref" != "" ]; then
            # output format
            echo "$nodeName"
            echo "^^^^^^^^^^^^"
            curl -s "${JENKINS_URL_CLEAN}$logHref"
        fi
    fi
fi