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

def call(config) {
    edgex.bannerMessage "[edgeXBuildCApp] RAW Config: ${config}"

    validate(config)
    edgex.setupNodes(config)

    def _envVarMap = toEnvironment(config)

    ///////////////////////////////////////////////////////////////////////////

    pipeline {
        agent {
            node {
                label edgex.mainNode(config)
            }
        }
        options {
            timestamps()
            preserveStashes()
            quietPeriod(5) // wait a few seconds before starting to aggregate builds...??
            durabilityHint 'PERFORMANCE_OPTIMIZED'
            timeout(360)
        }

        stages {
            stage('Prepare') {
                steps {
                    edgeXSetupEnvironment(_envVarMap)
                }
            }

            stage('Semver Prep') {
                when { environment name: 'USE_SEMVER', value: 'true' }
                steps {
                    edgeXSemver 'init' // <-- Generates a VERSION file and .semver directory
                }
            }

            stage('Build') {
                parallel {
                    stage('amd64') {
                        when {
                            beforeAgent true
                            expression { edgex.nodeExists(config, 'amd64') }
                        }
                        agent { // comment out to reuse mainNode
                            node {
                                label edgex.getNode(config, 'amd64')
                                customWorkspace "/w/workspace/${env.PROJECT}/${env.BUILD_ID}"
                            }
                        }
                        environment {
                            ARCH = 'x86_64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        // should this be in it's own stage?
                                        if(env.USE_SEMVER == 'true') {
                                            unstash 'semver'
                                        }
                                        prepBaseBuildImage()
                                    }
                                }
                            }

                            stage('Test') {
                                steps {
                                    script {
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            sh "${env.TEST_SCRIPT}"
                                        }
                                    }
                                }
                            }

                            stage('Build') {
                                when { environment name: 'BUILD_DOCKER_IMAGE', value: 'true' }
                                steps {
                                    script {
                                        edgeXDocker.build("${env.DOCKER_IMAGE_NAME}", "ci-base-image-${env.ARCH}")
                                    }
                                }
                            }

                            stage('Docker Push') {
                                when { 
                                    allOf {
                                        environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
                                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                                        expression { edgex.isReleaseStream() }
                                    }
                                }

                                steps {
                                    script {
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        edgeXDocker.push("${env.DOCKER_IMAGE_NAME}", true, "${DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }
                        }
                    }

                    stage('arm64') {
                        when {
                            beforeAgent true
                            expression { edgex.nodeExists(config, 'arm64') }
                        }
                        agent {
                            node {
                                label edgex.getNode(config, 'arm64')
                                customWorkspace "/w/workspace/${env.PROJECT}/${env.BUILD_ID}"
                            }
                        }
                        environment {
                            ARCH = 'arm64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        // should this be in it's own stage?
                                        if(env.USE_SEMVER == 'true') {
                                            unstash 'semver'
                                        }
                                        prepBaseBuildImage()
                                    }
                                }
                            }

                            stage('Test') {
                                steps {
                                    script {
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            sh "${env.TEST_SCRIPT}"
                                        }
                                    }
                                }
                            }

                            stage('Build') {
                                when { environment name: 'BUILD_DOCKER_IMAGE', value: 'true' }
                                steps {
                                    script {
                                        edgeXDocker.build("${env.DOCKER_IMAGE_NAME}-${env.ARCH}", "ci-base-image-${env.ARCH}")
                                    }
                                }
                            }

                            stage('Docker Push') {
                                when { 
                                    allOf {
                                        environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
                                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                                        expression { edgex.isReleaseStream() }
                                    }
                                }

                                steps {
                                    script {
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        edgeXDocker.push("${env.DOCKER_IMAGE_NAME}-${env.ARCH}", true, "${env.DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //////////////////////////////////////////////////////////////////
            // We should be back on the mainAgent here.

            // CodeCov should only run once, no need to run per arch only
            stage('CodeCov') {
                when { environment name: 'SILO', value: 'production' }
                steps {
                    unstash 'coverage-report'
                    edgeXCodecov "${env.PROJECT}-codecov-token"
                }
            }

            // Scan Docker Image Created
            stage('Snyk Scan') {
                when { 
                    allOf {
                        environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                        expression { edgex.isReleaseStream() }
                    }
                }
                steps {
                    script {
                        def amd64Image = edgeXDocker.finalImageName("${env.DOCKER_IMAGE_NAME}")
                        edgeXSnyk(dockerImage="${env.DOCKER_REGISTRY}/${amd64Image}:${env.GIT_COMMIT}")

                        def arm64Image = edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}-arm64")
                        edgeXSnyk(dockerImage="${env.DOCKER_REGISTRY}/${arm64Image}:${env.GIT_COMMIT}")
                    }
                }
            }

            // When scanning the clair image, the FQDN is needed
            stage('Clair Scan') {
                when {
                    allOf {
                        environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                        expression { edgex.isReleaseStream() }
                    }
                }
                steps {
                    script {
                        def amd64Image = edgeXDocker.finalImageName("${env.DOCKER_IMAGE_NAME}")
                        edgeXClair("${env.DOCKER_REGISTRY}/${amd64Image}:${env.GIT_COMMIT}")

                        def arm64Image = edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}-arm64")
                        edgeXClair("${env.DOCKER_REGISTRY}/${arm64Image}:${env.GIT_COMMIT}")
                    }
                }
            }

            stage('Semver') {
                when {
                    allOf {
                        environment name: 'USE_SEMVER', value: 'true'
                        expression { edgex.isReleaseStream() }
                    }
                }
                stages {
                    stage('Tag') {
                        steps {
                            unstash 'semver'

                            edgeXSemver 'tag'
                            edgeXInfraLFToolsSign(command: 'git-tag', version: 'v${VERSION}')
                        }
                    }
                    stage('Bump Pre-Release Version') {
                        steps {
                            edgeXSemver "bump ${env.SEMVER_BUMP_LEVEL}"
                            edgeXSemver 'push'
                        }
                    }
                }
            }
        }

        post {
            failure {
                script { currentBuild.result = "FAILED" }
            }
            always {
                edgeXInfraPublish()
            }
            cleanup {
                cleanWs()
            }
        }
    }
}

def prepBaseBuildImage() {
    def baseImage = env.DOCKER_BASE_IMAGE

    if(env.ARCH == 'arm64' && baseImage.contains(env.DOCKER_REGISTRY)) {
        baseImage = "${env.DOCKER_BASE_IMAGE}".replace('edgex-gcc-base', "edgex-gcc-base-${env.ARCH}")
    }

    edgex.bannerMessage "[edgeXBuildCApp] Building Code With image [${baseImage}]"

    def buildArgs = ['', "BASE=${baseImage}"]
    if(env.http_proxy) {
        buildArgs << "http_proxy"
        buildArgs << "https_proxy"
    }
    def buildArgString = buildArgs.join(' --build-arg ')

    docker.build(
        "ci-base-image-${env.ARCH}",
        "-f ${env.DOCKER_BUILD_FILE_PATH} ${buildArgString} ${env.DOCKER_BUILD_CONTEXT}"
    )
}

def validate(config) {
    if(!config.project) {
        error('[edgeXBuildCApp] The parameter "project" is required. This is typically the project name.')
    }
}

def toEnvironment(config) {
    def _projectName   = config.project
    def _mavenSettings = config.mavenSettings ?: "${_projectName}-settings"

    if(env.SILO == 'sandbox') {
        _mavenSettings = 'sandbox-settings'
    }

    def _useSemver      = edgex.defaultTrue(config.semver)
    def _testScript     = config.testScript ?: 'make test'
    def _buildScript    = config.buildScript ?: 'make build'

    def _dockerBaseImage     = config.dockerBaseImage ?: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest'
    def _dockerFilePath      = config.dockerFilePath ?: 'Dockerfile'
    def _dockerBuildFilePath = config.dockerBuildFilePath ?: 'Dockerfile.build'
    def _dockerBuildContext  = config.dockerBuildContext ?: '.'
    def _dockerNamespace     = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerImageName     = config.dockerImageName ?: "docker-${_projectName}"
    def _dockerNexusRepo     = config.dockerNexusRepo ?: 'staging'
    def _buildImage          = edgex.defaultTrue(config.buildImage)
    def _pushImage           = edgex.defaultTrue(config.pushImage)
    def _semverBump          = config.semverBump ?: 'pre'

    // no image to build, no image to push
    if(!_buildImage) {
        _pushImage = false
    }

    def envMap = [
        MAVEN_SETTINGS: _mavenSettings,
        PROJECT: _projectName,
        USE_SEMVER: _useSemver,
        TEST_SCRIPT: _testScript,
        BUILD_SCRIPT: _buildScript,
        DOCKER_BASE_IMAGE: _dockerBaseImage,
        DOCKER_FILE_PATH: _dockerFilePath,
        DOCKER_BUILD_FILE_PATH: _dockerBuildFilePath,
        DOCKER_BUILD_CONTEXT: _dockerBuildContext,
        DOCKER_IMAGE_NAME: _dockerImageName,
        DOCKER_REGISTRY_NAMESPACE: _dockerNamespace,
        DOCKER_NEXUS_REPO: _dockerNexusRepo,
        BUILD_DOCKER_IMAGE: _buildImage,
        PUSH_DOCKER_IMAGE: _pushImage,
        SEMVER_BUMP_LEVEL: _semverBump
    ]

    edgex.bannerMessage "[edgeXBuildCApp] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}