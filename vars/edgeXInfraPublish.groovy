import org.jenkinsci.plugins.workflow.libs.Library
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
@Library("lf-pipelines") _

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]

    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    def _logSettingsFile = config.logSettingsFile ?: 'jenkins-log-archives-settings'
    def _dockerOptimized = edgex.defaultTrue(config.dockerOptimized)

    stage('LF Post Build Actions') {
        // lf-infra-systat
        sh(script: libraryResource('global-jjb-shell/sysstat.sh'))

        // lf-infra-package-listing
        sh(script: libraryResource('global-jjb-shell/package-listing.sh'))

        if(_dockerOptimized) {
            def insideArgs = getLogPublishContainerArgs()
            // lf-infra-ship-logs
            sh 'facter operatingsystem > ./facter-os'
            docker.image("${env.DOCKER_REGISTRY}:10003/edgex-lftools-log-publisher:alpine").inside(insideArgs.join(' ')) {
                sh 'touch /tmp/pre-build-complete' // skips python-tools-install.sh

                // this will remap the sa logs from the host
                sh 'mkdir -p /var/log/sa'
                sh 'for file in `ls /var/log/sa-host`; do sadf -c /var/log/sa-host/${file} > /var/log/sa/${file}; done'
                //////////////////////////////////////////////

                lfInfraShipLogs {
                    logSettingsFile = _logSettingsFile
                }
            }
        } else {
            lfInfraShipLogs {
                logSettingsFile = _logSettingsFile
            }
        }

        cleanWs()
    }
}

def getLogPublishContainerArgs() {
    def insideArgs = [
        '--privileged',
        '-u 0:0',
        '--net host', // required for the calls to the metadata IP 169.254.169.254 global-jjb/shell/job-cost.sh

        // These are the required bind mounts for the scripts to work properly
        '-v /var/log/sa:/var/log/sa-host',          // global-jjb/shell/logs-deploy.sh
        '-v /var/log/secure:/var/log/secure',       // global-jjb/shell/sudo-logs.shgho
        '-v /var/log/auth.log:/var/log/auth.log',   // global-jjb/shell/sudo-logs.sh
        "-v ${env.WORKSPACE}/facter-os:/facter-os", // global-jjb/shell/sudo-logs.sh
        '-v /proc/uptime:/proc/uptime',             // global-jjb/shell/job-cost.sh
        '-v /run/cloud-init/result.json:/run/cloud-init/result.json' // global-jjb/shell/job-cost.sh
    ]

    println "Launching container with: [${insideArgs.join(' ')}]"

    insideArgs
}