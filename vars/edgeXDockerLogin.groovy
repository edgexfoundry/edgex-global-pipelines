//
// Copyright (c) 2019 Intel Corporation
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
 # edgeXDockerLogin
 
 ## Overview

 Shared library that wraps the Linux Foundation's (LF) docker login script: `./resources/global-jjb/shell/docker-login.sh`. 
 
 The LF Global JJB Docker Login script looks for the following environment variables: 
 `$SETTINGS_FILE`, `$DOCKER_REGISTRY`, `$REGISTRY_PORTS`, `$DOCKERHUB_REGISTRY`, `$DOCKERHUB_EMAIL` which this script automatically sets. Based on given config map entries passed in
 
 Please refer to the shell script in global-jjb/shell for the usage.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 settingsFile | true | str | Config file Id that contains authentication details to docker registries. Unique to each Edgex repository. |
 dockerRegistry | false | str | Override docker registry to login to. |
 dockerRegistryPorts | false | str | Listing of all the registry ports to login to e.g.: `10001 10002 10003 10004` |
 dockerHubRegistry | false | str | Override docker hub registry. Not commonly used. |
 dockerHubEmail | false | str | Override docker hub email. Not commonly used. |

 ## Usage

 ```groovy
 edgeXDockerLogin(settingsFile: 'edgex-repo-settings')
 ```
*/

def call(config = [:]) {
    def _dockerRegistry = config.dockerRegistry ?: ''
    def _dockerRegistryPorts = config.dockerRegistryPorts ?: ''
    def _dockerHubRegistry = config.dockerHubRegistry ?: ''
    def _dockerHubEmail = config.dockerHubEmail ?: ''

    def _settingsFile = config.settingsFile
    if(!_settingsFile) {
        error('Project Settings File id (settingsFile) is required for the docker login script.')
    }

    if(_dockerRegistry && !_dockerRegistryPorts) {
        error('Docker registry ports (dockerRegistryPorts) are required when docker registry is set (dockerRegistry).')
    }

    if(_dockerRegistryPorts && !_dockerRegistry) {
        error('Docker registry (dockerRegistry) is required when docker registry ports are set (dockerRegistryPorts).')
    }

    def envVars = []
    if(_dockerRegistry)      { envVars << "DOCKER_REGISTRY=${_dockerRegistry}" }
    if(_dockerRegistryPorts) { envVars << "REGISTRY_PORTS=${_dockerRegistryPorts}" }
    if(_dockerHubRegistry)   { envVars << "DOCKERHUB_REGISTRY=${_dockerHubRegistry}" }
    if(_dockerHubEmail)      { envVars << "DOCKERHUB_EMAIL=${_dockerHubEmail}" }

    withEnv(envVars){
        configFileProvider([configFile(fileId: _settingsFile, variable: 'SETTINGS_FILE')]) {
            sh(script: libraryResource('global-jjb-shell/docker-login.sh'))
        }
    }
}