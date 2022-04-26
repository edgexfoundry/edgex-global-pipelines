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
 # edgeXNexusPublish

 ## Overview

 Shared library to publish a ZIP file to a specific nexus repo.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 serverId | true | str | Used to lookup credentials in mavenSettings file.<br /><br />**Example:** `logs`, `docker`, `nexus.edgexfoundry.org` |
 mavenSettings | true | str | Config file Id to use publish to Nexus.<br /><br />**Example:** `log-settings` |
 nexusRepo | true | str | The nexus repository name where you would like to publish your artifacts. |
 nexusPath | false | str | Path on the nexus server where file should be stored. <br /><br />**Default:**`${env.SILO}/${env.JENKINS_HOSTNAME}/${env.JOB_NAME}/${env.BUILD_NUMBER}` |
 zipFilePath | true | str | path to ZIP file, typically in the workspace. |

 ## Usage

 ```groovy
 edgeXNexusPublish([serverId: 'logs', mavenSettings: 'log-settings', nexusRepo: 'logs', zipFilePath: '*.zip'])
 ```
*/

def call(Map config = [:]) {
    def serverId      = config.serverId
    def mavenSettings = config.mavenSettings
    def nexusRepo     = config.nexusRepo
    def nexusPath     = config.nexusPath ?: "${env.SILO}/${env.JENKINS_HOSTNAME}/${env.JOB_NAME}/${env.BUILD_NUMBER}"
    def zipFilePath   = config.zipFilePath

    def vmArch        = sh(script: 'uname -m', returnStdout: true).trim() ?: 'x86_64'
    def lftoolsImage  = "${env.DOCKER_REGISTRY}:10003/edgex-lftools-log-publisher:${vmArch}"

    if(!serverId) {
        error("[edgeXNexusPublish] serverId is required to publish to nexus. Example: 'logs'")
    }

    if(!mavenSettings) {
        error("[edgeXNexusPublish] mavenSettings is required to publish to nexus. Example: 'sandbox-settings'")
    }

    if(!nexusRepo) {
        error("[edgeXNexusPublish] nexusRepo is required to publish to nexus. Example: 'logs'")
    }

    if(!zipFilePath) {
        error("[edgeXNexusPublish] zipFilePath is required to publish to nexus. Example: '**/*.zip'")
    }

    def publishEnv = [
        "SERVER_ID=${serverId}",
        "NEXUS_REPO=${nexusRepo}",
        "NEXUS_PATH=${nexusPath}"
    ]

    println "[edgeXNexusPublish] Looking for ZIP files to publish: [${zipFilePath}]"
    def zipFiles = findFiles(glob: zipFilePath)

    if(zipFiles.length > 0) {
        docker.image(lftoolsImage).inside('-u 0:0') {
            withEnv(publishEnv) {
                configFileProvider([
                    configFile(fileId: mavenSettings, variable: 'SETTINGS_FILE')
                ]) {
                    sh(script: libraryResource('global-jjb-shell/create-netrc.sh'))

                    zipFiles.each { file ->
                        sh "lftools deploy nexus-zip ${env.NEXUS_URL} ${env.NEXUS_REPO} ${env.NEXUS_PATH} ${file}"
                    }
                }
            }
        }
    }
    else {
        println "[edgeXNexusPublish] Could not find any ZIP files to publish"
    }
}