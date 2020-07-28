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

    stage('LF Post Build Actions') {
        // lf-infra-systat
        sh(script: libraryResource('global-jjb-shell/sysstat.sh'))

        // lf-infra-package-listing
        sh(script: libraryResource('global-jjb-shell/package-listing.sh'))

        // lf-infra-ship-logs
        edgeXInfraShipLogs {
            logSettingsFile = _logSettingsFile
        }

        cleanWs()
    }
}