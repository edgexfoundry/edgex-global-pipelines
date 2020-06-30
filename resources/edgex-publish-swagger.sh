#!/bin/bash
set -e -o pipefail
echo "--> edgex-go-publish-swagger.sh"
# if no ARCH is set or ARCH is not arm
if [ -z "$ARCH" ] || [ "$ARCH" != "arm64" ] ; then
    # NOTE: APIKEY needs to be a pointer to a file with the key. This will need to be set locally from your environment or from Jenkins
    APIKEY_VALUE=`cat $APIKEY`
    # Upload both API versions,v1 and v2, to SwaggerHub
    API_FOLDERS="v1 v2"
    SWAGGER_DRY_RUN=${SWAGGER_DRY_RUN:-false}
    SCRIPTS_ROOT="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
    . $SCRIPTS_ROOT/toSwaggerHub.sh
    OASVERSION='3.0.0'
    ISPRIVATE=false
    OWNER=${1:-EdgeXFoundry1}
    for API_FOLDER in ${API_FOLDERS}; do
        echo "=== Publish ${API_FOLDER} API ==="
        publishToSwagger "${APIKEY_VALUE}" "${API_FOLDER}" "${OASVERSION}" "${ISPRIVATE}" "${OWNER}" "${SWAGGER_DRY_RUN}"
    done
else
    echo "$ARCH not supported...skipping."
fi