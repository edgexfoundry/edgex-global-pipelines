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

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    
    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    def _logSettingsFile = config.logSettingsFile ?: 'jenkins-log-archives-settings'
    if(!_logSettingsFile) {
        throw new Exception('Log settings file id (logSettingsFile) is required for LF log deploy script.')
    }

    // running this inside the lftools container to avoid the 1-3 minute install of lftools
    docker.image("${env.DOCKER_REGISTRY}:10003/edgex-lftools:latest").inside('-u 0:0') {
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
    if(env.ghprbPullId) {
        currentBuild.description += "<br>"
    }
    currentBuild.description += "Build logs: <a href=\"$LOGS_SERVER/$SILO/$JENKINS_HOSTNAME/$JOB_NAME/$BUILD_NUMBER\">$LOGS_SERVER/$SILO/$JENKINS_HOSTNAME/$JOB_NAME/$BUILD_NUMBER</a>"
}