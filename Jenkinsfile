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
                script {
                    //edgex.releaseInfo() this can be uncommented once this moves to stable
                    def branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    def commit = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()

                    echo "================================"
                    env.setProperty('GIT_BRANCH', branch)
                    env.setProperty('GIT_COMMIT', commit)
                    sh 'env | sort'
                    echo "is release stream [${edgex.isReleaseStream()}]"
                    echo "================================"

                    edgeXSetupEnvironment()
                    edgeXSemver 'init'

                    env.OG_VERSION = env.VERSION
                    sh "echo Archived original version: [$OG_VERSION]"
                }
            }
        }

        stage('Lint Pipelines') {
            steps {
                sh './scripts/linter.sh'
            }
        }

        stage('Test') {
            when { expression { !edgex.isReleaseStream() } }
            agent {
                docker {
                    image "${DOCKER_REGISTRY}:10003/edgex-devops/egp-unit-test:gradle"
                    args '-u 0:0 --privileged'
                    reuseNode true
                }
            }
            steps {
                sh 'gradle -Dgradle.user.home=/gradleCache clean test'

                junit allowEmptyResults: true, testResults: 'target/test-results/test/*.xml'

                // Test summary
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/reports/tests/test',
                    reportFiles: 'index.html',
                    reportName: 'Unit Test Summary'
                ])

                // // Jacoco Report
                // publishHTML([
                //     allowMissing: true,
                //     alwaysLinkToLastBuild: true,
                //     keepAll: true,
                //     reportDir: 'target/reports/jacoco/test/html',
                //     reportFiles: 'index.html',
                //     reportName: 'Jacoco Test Report'
                // ])
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
                edgeXSemver('bump patch') //this changes the VERSION env var
                edgeXSemver('push')
            }
        }

        // automatically bump experimental tag...more research needed
        stage('🧪 Bump Experimental Tag') {
            when { expression { edgex.isReleaseStream() } }
            steps {
                sshagent (credentials: ['edgex-jenkins-ssh']) {
                    sh 'echo y | ./scripts/update-named-tag.sh "v${OG_VERSION}" "experimental"'
                }
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