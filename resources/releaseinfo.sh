#!/bin/bash
set +e #prevents exit of script due to command error

GH_BASE_URL=${GH_BASE_URL:-https://api.github.com}
GH_TOKEN=${GH_TOKEN_PSW}

tagsToDisplay="${EGP_TAG_VERSIONS:-stable experimental}"
for namedTag in $(echo $tagsToDisplay); do
    allTags=$(curl -u "$GH_TOKEN:x-oauth-basic" -s "$GH_BASE_URL/repos/edgexfoundry/edgex-global-pipelines/git/refs/tags")

    #find the named tag commit sha
    searchSha=$(echo "$allTags" | jq -r ".[] | select(.ref==\"refs/tags/${namedTag}\") | .object.sha")
    tagJson=$(curl -u "$GH_TOKEN:x-oauth-basic" -s "$GH_BASE_URL/repos/edgexfoundry/edgex-global-pipelines/git/tags/${searchSha}")

    if [ "$(echo "$tagJson" | tr -d '\n' | jq -r '.message')" != "Not Found" ]; then
    tagger=$(echo "$tagJson"    | tr -d '\n' | jq -r '.tagger.name + " " + .tagger.email')
    message=$(echo "$tagJson"   | tr -d '\n' | jq -r '.message')
    commitSha=$(echo "$tagJson" | tr -d '\n' | jq -r '.object.sha')

    cat << EOF
-------------------
$namedTag info:
-------------------
Commited By: $tagger
Commit SHA: $commitSha
Message: $message
EOF
    else
        echo '-------------------'
        echo "No tag info for: $namedTag"
    fi
done