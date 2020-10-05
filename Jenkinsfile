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

// remove once stable tag is moved to include the edgeXUpdateNamedTag changes
@Library("edgex-global-pipelines@experimental") _

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

    environment {
        SEMVER_BRANCH = 'master'
    }

    stages {
        stage('Prep') {
            steps {
                script {
                    //edgex.releaseInfo() this can be uncommented once this moves to stable
                    //edgeXSetupEnvironment([GIT_BRANCH: env.BRANCH_NAME])
                    edgeXSemver 'init'

                    env.OG_VERSION = env.VERSION
                    sh "echo Archived original version: [$OG_VERSION]"

                    sh 'env | sort'
                }
            }
        }

        stage('Lint Pipelines') {
            when { not { expression { env.BRANCH_NAME =~ /^master$/ } } }
            steps {
                sh './scripts/linter.sh'
            }
        }

        stage('Generate Documentation') {
            when { { expression { env.BRANCH_NAME =~ /^master$/ } } }
            agent {
                docker {
                    image 'gradle'
                    reuseNode true
                    args '-u 0:0 --privileged'
                }
            }
            steps {
                sh 'gradle clean generateDocumentation'
                // stash the site contents generated from gradle tasks
                stash name: 'md_files', includes: 'docs_src/md_files/**', useDefaultExcludes: false
            }
        }
        stage('MkDocs Build') {
            when { { expression { env.BRANCH_NAME =~ /^master$/ } } }
            agent {
                docker {
                    image 'python:3-slim'
                    reuseNode true
                    args '-u 0:0 --privileged'
                }
            }
            steps {
                sh 'pip install mkdocs'
                sh 'pip install mkdocs-material'
                dir('docs_src') {
                    unstash 'md_files'
                }
                sh 'mkdocs build'
                // stash the site contents generated from mkdocs build
                stash name: 'html_files', includes: 'docs/html/**', useDefaultExcludes: false
            }
        }
        stage('Publish to GitHub pages') {
            when { { expression { env.BRANCH_NAME =~ /^master$/ } } }
            agent {
                docker {
                    image 'gradle'
                    reuseNode true
                    args '-u 0:0 --privileged'
                }
            }
            steps {
                sh 'gradle publish'
            }
        }

        stage('Test') {
            when { not { expression { env.BRANCH_NAME =~ /^master$/ } } }
            agent {
                docker {
                    image "${DOCKER_REGISTRY}:10003/edgex-devops/egp-unit-test:gradle"
                    args '-u 0:0 --privileged'
                    reuseNode true
                }
            }
            steps {
                sh 'gradle -Dgradle.user.home=/gradleCache clean test --parallel'

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
//
//                // // Jacoco Report
//                // publishHTML([
//                //     allowMissing: true,
//                //     alwaysLinkToLastBuild: true,
//                //     keepAll: true,
//                //     reportDir: 'target/reports/jacoco/test/html',
//                //     reportFiles: 'index.html',
//                //     reportName: 'Jacoco Test Report'
//                // ])
            }
        }

        stage('Semver Tag') {
            when { expression { env.BRANCH_NAME =~ /^master$/ } }
            steps {
                sh 'echo v${VERSION}'
                edgeXSemver('tag')
                edgeXInfraLFToolsSign(command: 'git-tag', version: 'v${VERSION}')
            }
        }

        stage('Semver Bump Pre-Release Version') {
            when {
                allOf {
                    expression { env.BRANCH_NAME =~ /^master$/ }
                    // env.GITSEMVER_HEAD_TAG is only set when HEAD is tagged and
                    // when set - edgeXSemver will ignore all tag, bump and push commands (unforced)
                    // thus we also want to ignore updating stable/experimental tags when set
                    expression { env.GITSEMVER_HEAD_TAG == null }
                }
            }
            steps {
                edgeXSemver('bump patch') //this changes the VERSION env var
                edgeXSemver('push')
            }
        }

        // automatically bump experimental tag...more research needed
        stage('🧪 Bump Experimental Tag') {
            when {
                allOf {
                    expression { env.BRANCH_NAME =~ /^master$/ }
                    // env.GITSEMVER_HEAD_TAG is only set when HEAD is tagged and
                    // when set - edgeXSemver will ignore all tag, bump and push commands (unforced)
                    // thus we also want to ignore updating stable/experimental tags when set
                    expression { env.GITSEMVER_HEAD_TAG == null }
                }
            }
            steps {
                script {
                    edgeXUpdateNamedTag(env.OG_VERSION, 'experimental')
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
