#!/bin/bash
set +e #prevents exit of script due to command error
tagsToDisplay="${EGP_TAG_VERSIONS:-stable experimental}"
for tagSearch in $(echo $tagsToDisplay);
do 
    allTags=$(curl -s "https://api.github.com/repos/edgexfoundry/edgex-global-pipelines/git/refs/tags");
    searchSha=$(echo "$allTags" | jq -r ".[] | select(.ref==\"refs/tags/${tagSearch}\") | .object.sha"); 
    tagJson=$(curl -s "https://api.github.com/repos/edgexfoundry/edgex-global-pipelines/git/tags/${searchSha}"); 
    tagger=$(echo $tagJson | tr -d '\n' | jq -r '.tagger.name + " " + .tagger.email'); 
    message=$(echo $tagJson | tr -d '\n' | jq -r '.message'); 
    commitSha=$(echo "$tagJson" | tr -d '\n' | jq -r '.object.sha') ; 
    echo "$tagSearch info:\n-------------------\nCommited By: $tagger\nCommit SHA: $commitSha\nMessage: $message\n-------------------"; 
done