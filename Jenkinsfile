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

loadGlobalLibrary()

pipeline {
    agent {
        label 'centos7-docker-4c-2g'
    }

    options {
        timestamps()
        preserveStashes()
        quietPeriod(5) // wait a few seconds before starting to aggregate builds...??
        durabilityHint 'PERFORMANCE_OPTIMIZED'
        timeout(360)
    }

    stages {
        stage('Prep') {
            steps {
                edgeXSetupEnvironment()
                edgeXSemver 'init'
            }
        }

        stage('Test') {
            when { expression { !edgex.isReleaseStream() } }
            agent {
                docker {
                    image "${env.DOCKER_REGISTRY}:10003/edgex-devops/egp-unit-test:latest"
                    reuseNode true
                }
            }
            steps {
                sh 'mvn clean test'
            }
        }

        stage('Semver Tag') {
            when { expression { edgex.isReleaseStream() } }
            steps {
                sh 'echo v${VERSION}'
                edgeXSemver('tag')
                edgeXInfraLFToolsSign(command: 'git-tag', version: 'v${VERSION}')
            }
        }

        stage('Semver Bump Pre-Release Version') {
            when { expression { edgex.isReleaseStream() } }
            steps {
                edgeXSemver('bump patch')
                edgeXSemver('push')
            }
        }

        // automatically bump experimental tag
         stage('🧪 Bump Experimental Tag') {
            when { expression { edgex.isReleaseStream() } }
            steps {
                sh 'echo y | ./scripts/update-named-tag.sh "v${VERSION}" "experimental"'
            }
        }
    }

    post {
        failure {
            script {
                currentBuild.result = "FAILED"
            }
        }
        always {
            edgeXInfraPublish()
        }
    }
}

def loadGlobalLibrary(branch = '*/master') {
    library(identifier: 'edgex-global-pipelines@master', 
        retriever: legacySCM([
            $class: 'GitSCM',
            userRemoteConfigs: [[url: 'https://github.com/edgexfoundry/edgex-global-pipelines.git']],
            branches: [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
                $class: 'SubmoduleOption',
                recursiveSubmodules: true,
            ]]]
        )
    ) _
}
