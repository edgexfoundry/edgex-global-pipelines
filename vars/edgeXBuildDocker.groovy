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
 #edgeXBuildDocker

 Shared Library to build docker images

 ##Parameters

 * **project** - Specify your project name
 * **dockerImageName** - Specify your Docker Image name
 * **semver** - Set this `true` for Semantic versioning
 * **dockerNexusRepo** - Specify Docker Nexus repository

 ##Example Usage

 ```bash
 edgeXBuildDocker (
 project: 'docker-edgex-consul',
 dockerImageName: 'docker-edgex-consul',
 semver: true
 )
 ```
 ```bash
 edgeXBuildDocker (
 project: 'edgex-taf-common',
 mavenSettings: 'taf-settings',
 dockerNexusRepo: 'snapshots',
 semver: true
 )
 ```
 */
def taggedAMD64Images
def taggedARM64Images

def call(config) {
    edgex.bannerMessage "[edgeXBuildDocker] RAW Config: ${config}"

    validate(config)
    edgex.setupNodes(config)

    def _envVarMap = toEnvironment(config)

    ///////////////////////////////////////////////////////////////////////////

    pipeline {
        agent { label edgex.mainNode(config) }

        options {
            timestamps()
            preserveStashes()
            quietPeriod(5) // wait a few seconds before starting to aggregate builds...??
            durabilityHint 'PERFORMANCE_OPTIMIZED'
            timeout(360)
        }

        triggers {
            issueCommentTrigger('.*^recheck$.*')
        }

        stages {
            stage('Prepare') {
                steps {
                    script { edgex.releaseInfo() }
                    edgeXSetupEnvironment(_envVarMap)
                }
            }

            stage('Semver Prep') {
                when { environment name: 'USE_SEMVER', value: 'true' }
                steps {
                    edgeXSemver 'init' // <-- Generates a VERSION file and .semver directory
                }
            }

            stage('Build Docker Image') {
                parallel {
                    stage('amd64') {
                        when {
                            beforeAgent true
                            expression { edgex.nodeExists(config, 'amd64') }
                        }
                        // agent { label edgex.getNode(config, 'amd64') } // comment out to reuse mainNode
                        environment {
                            ARCH = 'x86_64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        if(env.USE_SEMVER == 'true') {
                                            unstash 'semver'
                                        }
                                    }
                                }
                            }

                            stage('Docker Build') {
                                steps {
                                    script {
                                        edgeXDocker.build("${DOCKER_IMAGE_NAME}")
                                    }
                                }
                            }

                            stage('Docker Push') {
                                when {
                                    allOf {
                                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                                        expression { edgex.isReleaseStream() || (env.GIT_BRANCH == env.RELEASE_BRANCH_OVERRIDE) }
                                    }
                                }

                                steps {
                                    script {
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        taggedAMD64Images = edgeXDocker.push("${DOCKER_IMAGE_NAME}", env.DOCKER_PUSH_LATEST == 'true', "${DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }

                            stage('Archive Image') {
                                when {
                                    environment name: 'ARCHIVE_IMAGE', value: 'true'
                                }
                                
                                steps {
                                    script {
                                        withEnv(["IMAGE=${edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}")}"]) {
                                            // save the docker image to tar file
                                            sh 'docker save -o $WORKSPACE/$(printf \'%s\' "${ARCHIVE_NAME%.tar.gz}-$ARCH.tar.gz") ${IMAGE}'
                                            archiveArtifacts allowEmptyArchive: true, artifacts: '*.tar.gz', fingerprint: true, onlyIfSuccessful: true
                                        }
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
                        agent { label edgex.getNode(config, 'arm64') }
                        environment {
                            ARCH = 'arm64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        if(env.USE_SEMVER == 'true') {
                                            unstash 'semver'
                                        }
                                    }
                                }
                            }

                            stage('Docker Build') {
                                steps {
                                    script {
                                        edgeXDocker.build("${DOCKER_IMAGE_NAME}-${ARCH}")
                                    }
                                }
                            }

                            stage('Docker Push') {
                                when {
                                    allOf {
                                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                                        expression { edgex.isReleaseStream() || (env.GIT_BRANCH == env.RELEASE_BRANCH_OVERRIDE) }
                                    }
                                }

                                steps {
                                    script {
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        taggedARM64Images = edgeXDocker.push("${DOCKER_IMAGE_NAME}-${ARCH}", env.DOCKER_PUSH_LATEST == 'true', "${DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }

                            stage('Archive Image') {
                                when {
                                    environment name: 'ARCHIVE_IMAGE', value: 'true'
                                }
                                
                                steps {
                                    script {
                                        withEnv(["IMAGE=${edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}-${ARCH}")}"]) {
                                            // save the docker image to tar file
                                            sh 'docker save -o $WORKSPACE/$(printf \'%s\' "${ARCHIVE_NAME%.tar.gz}-$ARCH.tar.gz") ${IMAGE}'
                                            archiveArtifacts allowEmptyArchive: true, artifacts: '*.tar.gz', fingerprint: true, onlyIfSuccessful: true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snyk doesn't support general docker images - and doesn't support C
            // stage('Snyk Scan') {
            //     when { 
            //         allOf {
            //             environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
            //             environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
            //             expression { edgex.isReleaseStream() }
            //         }
            //     }
            //     steps {
            //         script {
            //             if(edgex.nodeExists(config, 'amd64') && taggedAMD64Images){
            //                 edgeXSnyk(taggedAMD64Images.first(), env.DOCKER_FILE_PATH)
            //             }
            //             if(edgex.nodeExists(config, 'arm64') && taggedARM64Images){
            //                 edgeXSnyk(taggedARM64Images.first(), env.DOCKER_FILE_PATH)
            //             }
            //         }
            //     }
            // }

            // When scanning the clair image, the FQDN is needed
            stage('Clair Scan') {
                when {
                    allOf {
                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                        expression { edgex.isReleaseStream() || (env.GIT_BRANCH == env.RELEASE_BRANCH_OVERRIDE) }
                    }
                }
                steps {
                    script {
                        if(edgex.nodeExists(config, 'amd64') && taggedAMD64Images) {
                            edgeXClair(taggedAMD64Images.first())
                        }
                        if(edgex.nodeExists(config, 'arm64') && taggedARM64Images) {
                            edgeXClair(taggedARM64Images.first())
                        }
                    }
                }
            }

            stage('Semver') {
                when {
                    allOf {
                        environment name: 'USE_SEMVER', value: 'true'
                        expression { edgex.isReleaseStream() || (env.GIT_BRANCH == env.RELEASE_BRANCH_OVERRIDE) }
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

def validate(config) {
    if(!config.project) {
        error('[edgeXBuildDocker] The parameter "project" is required. This is typically the project name')
    }
}

def toEnvironment(config) {
    def _projectName   = config.project
    def _mavenSettings = config.mavenSettings ?: "${_projectName}-settings"

    if(env.SILO == 'sandbox') {
        _mavenSettings = 'sandbox-settings'
    }

    // default false
    def _useSemver     = edgex.defaultFalse(config.semver)

    // setup default values
    def _dockerFilePath        = config.dockerFilePath ?: 'Dockerfile'
    def _dockerBuildContext    = config.dockerBuildContext ?: '.'
    def _dockerBuildArgs       = config.dockerBuildArgs ?: []
    def _dockerNamespace       = config.dockerNamespace ?: ''
    def _dockerImageName       = config.dockerImageName ?: "docker-${_projectName}"
    def _dockerTags            = config.dockerTags ?: []
    def _dockerPushLatest      = edgex.defaultTrue(config.dockerPushLatest)
    def _dockerNexusRepo       = config.dockerNexusRepo ?: 'staging'
    def _pushImage             = edgex.defaultTrue(config.pushImage)
    def _archiveImage          = edgex.defaultFalse(config.archiveImage)
    def _archiveName           = config.archiveName ?: "${_projectName}-archive.tar.gz"
    def _semverBump            = config.semverBump ?: 'pre'
    def _releaseBranchOverride = config.releaseBranchOverride

    def envMap = [
        MAVEN_SETTINGS: _mavenSettings,
        PROJECT: _projectName,
        USE_SEMVER: _useSemver,
        DOCKER_FILE_PATH: _dockerFilePath,
        DOCKER_BUILD_CONTEXT: _dockerBuildContext,
        DOCKER_IMAGE_NAME: _dockerImageName,
        DOCKER_REGISTRY_NAMESPACE: _dockerNamespace,
        DOCKER_NEXUS_REPO: _dockerNexusRepo,
        DOCKER_PUSH_LATEST: _dockerPushLatest,
        PUSH_DOCKER_IMAGE: _pushImage,
        ARCHIVE_IMAGE: _archiveImage,
        ARCHIVE_NAME: _archiveName,
        SEMVER_BUMP_LEVEL: _semverBump
    ]

    if(_releaseBranchOverride) {
        envMap << [ RELEASE_BRANCH_OVERRIDE: _releaseBranchOverride ]
    }

    if(_dockerTags) {
        envMap << [ DOCKER_CUSTOM_TAGS: _dockerTags.join(' ') ]
    }

    // encode with comma in case build arg has space
    if(_dockerBuildArgs) {
        envMap << [ DOCKER_BUILD_ARGS: _dockerBuildArgs.join(',')]
    }

    edgex.bannerMessage "[edgeXBuildDocker] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}
