#!/bin/bash

echo "--> toSwaggerHub.sh"

publishToSwagger() {
    apiKey=$1
    apiFolder=$2
    oasVersion=$3
    isPrivate=$4
    owner=$5
    dryRun=${6:-false}

    apiPath="$WORKSPACE/api/openapi/"${apiFolder}

    echo "[toSwaggerHub] Publishing the API Docs [${apiFolder}] to Swagger"

    if [ -d "$apiPath" ]; then
        for file in "${apiPath}"/*.yaml; do
            apiName=$(basename "${file}" | cut -d "." -f 1)
            apiContent=$(cat "${apiPath}/${apiName}".yaml)

            echo "[toSwaggerHub] Publishing API Name [$apiName]"

            if [ "$dryRun" == "false" ]; then
                curl -X POST "https://api.swaggerhub.com/apis/${owner}/${apiName}?oas=${oasVersion}&isPrivate=${isPrivate}&force=true" \
                    -H "accept:application/json" \
                    -H "Authorization:${apiKey}" \
                    -H "Content-Type:application/yaml" \
                    -d "${apiContent}"
                echo $'\n'
            else
                echo "[toSwaggerHub] Dry Run enabled...Simulating upload"
                echo "curl -X POST https://api.swaggerhub.com/apis/${owner}/${apiName}?oas=${oasVersion}&isPrivate=${isPrivate}&force=true"
            fi

        done
    else
        echo "Could not find API Folder [${apiPath}]. Please make sure the API version exists..."
        exit 1
    fi
}
