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

// NOTE: This file is no longer in use. This has been replace by lfInfraShipLogs
// https://github.com/lfit/releng-pipelines/blob/master/vars/lfInfraShipLogs.groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    
    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    def _logSettingsFile = config.logSettingsFile ?: 'jenkins-log-archives-settings'

    // running this inside the lftools container to avoid the 1-3 minute install of lftools
    // Dockerfile for this image can be found here: https://github.com/edgexfoundry/ci-build-images/blob/lftools/Dockerfile.logs-publish
    docker.image("${env.DOCKER_REGISTRY}:10003/edgex-lftools-log-publisher:alpine").inside('--privileged -u 0:0 -v /var/log/sa:/var/log/sa-host') {
        // A conversion needs to take place for the sysstat files (sar is called during the lftools deploy logs)
        // See: https://serverfault.com/questions/757771/how-to-read-sar-file-from-different-ubuntu-system

        sh 'mkdir -p /var/log/sa'
        sh 'for file in `ls /var/log/sa-host`; do sadf -c /var/log/sa-host/${file} > /var/log/sa/${file}; done'

        withEnv(["SERVER_ID=logs"]){
            configFileProvider([configFile(fileId: _logSettingsFile, variable: 'SETTINGS_FILE')]) {
                echo 'Running global-jjb/create-netrc.sh'
                sh(script: libraryResource('global-jjb-shell/create-netrc.sh'))
                echo 'Running global-jjb/logs-deploy.sh'
                sh(script: libraryResource('global-jjb-shell/logs-deploy.sh'))
                echo 'Running global-jjb/logs-clear-credentials.sh'
                sh(script: libraryResource('global-jjb-shell/logs-clear-credentials.sh'))
            }
        }
    }

    // Set build description with build logs and PR info if applicable
    if(!currentBuild.description) {currentBuild.description = ''}

    // the old GHPRB plugin updated the description to contain the PR # with a link to the PR.
    // If the build description contains a link to the PR then add a br
    if(currentBuild.description.contains('PR #')) {
        currentBuild.description += "<br>"
    }
    currentBuild.description += "Build logs: <a href=\"${env.LOGS_SERVER}/${env.SILO}/${env.JENKINS_HOSTNAME}/${env.JOB_NAME}/${env.BUILD_NUMBER}\">${env.LOGS_SERVER}/${env.SILO}/${env.JENKINS_HOSTNAME}/${env.JOB_NAME}/${env.BUILD_NUMBER}</a>"
}