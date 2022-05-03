//
// Copyright (c) 2020 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

/**
 # edgeXSwaggerPublish

 ## Overview

 Shared library containing methods to publish Swagger API docs up to [https://api.swaggerhub.com](https://api.swaggerhub.com).

 **NOTE:** `${APIKEY}` needs to be a pointer to a file with the key. This will need to be set locally from your environment or from Jenkins.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 owner | false | str | Specify Swagger API owner. <br /><br />**Default:** `EdgeXFoundry1` |
 apiFolders | true | string | Space delimited list of folders to publish. |
 swaggerCredentialId | false | string | Config file Id that contains the Swagger API key to allow publishing of API docs. <br /><br />**Default:** `swaggerhub-api-key` |

 ## Usage

 Publish single folder to Swagger to owner:EdgeXFoundry1

 ```groovy
 edgeXSwaggerPublish(apiFolders: 'openapi/v1')
 ```

 Publish multiple API folders to Swagger to owner:EdgeXFoundry1

 ```groovy
 edgeXSwaggerPublish(apiFolders: 'openapi/v1 openapi/v2')
 ```

 Publish single folder to swagger with a customer owner/organization.

 ```groovy
 edgeXSwaggerPublish(owner: 'customOwner', apiFolders:'openapi/v1')
 ```
*/

def call(Map config = [:]) {
    def owner               = config.owner ?: 'EdgeXFoundry1'
    def apiFolders          = config.apiFolders ?: null
    def swaggerCredentialId = config.swaggerCredentialId ?: 'swaggerhub-api-key'
    def dryRun              = ['1', 'true'].contains(env.DRY_RUN)

    if (apiFolders == null){
        error("[edgeXSwaggerPublish]: No list of API Folders given")
    }
    
    try{
        println("[edgeXSwaggerPublish]: Setting up Environment and Executing Publish. - DRY_RUN: ${dryRun}")
        withEnv(["SWAGGER_DRY_RUN=${dryRun}",
                "OWNER=${owner}",
                "API_FOLDERS=${apiFolders}"
        ]) {
            configFileProvider([configFile(fileId: swaggerCredentialId, variable: 'APIKEY')]) {
                sh(script: libraryResource('edgex-publish-swagger.sh'))
            }
        }
    }
    catch(Exception ex) {
        error("[edgeXSwaggerPublish]: ERROR occurred publishing to SwaggerHub: ${ex}")
    }
}