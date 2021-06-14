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
 # edgeXBuildDocker

 Shared Library to build docker images

 ## Overview

 ![edgeXBuildDocker](images/edgeXBuildDocker.png)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 project | required | str | The name of your project. |
 mavenSettings | optional | str | The maven settings file in Jenkins that has been created for your project. **Note** the maven settings file specified must exist in Jenkins in order for your project to build.<br /><br />**Default**: `${project}-settings`
 semver | optional | bool | Specify if semantic versioning will be used to version your project. **Note** edgeX utilizes [git-semver](https://github.com/edgexfoundry/git-semver) for semantic versioning.<br /><br />**Default**: `true`
 dockerFilePath | optional | str | The path to the Dockerfile for your project.<br /><br />**Default**: `Dockerfile`
 dockerBuildContext | optional | str | The path for Docker to use as its build context when building your project. This applies to building both the CI build image and project image.<br /><br />**Default**: `.`
 dockerBuildArgs | optional | list | The list of additonal arguments to pass to Docker when building the image for your project.<br /><br />**Default**: `[]`
 dockerNamespace | optional | str | The docker registry namespace to use when publishing Docker images. **Note** for EdgeX projects images are published to the root of the docker registry and thus the namespace should be empty.<br /><br />**Default**: `''`
 dockerImageName | optional | str | The name of the Docker image for your project.<br /><br />**Default**: `docker-${project}`
 dockerTags | optional | str | The tag name for your docker image.<br /><br />**Default**: `[]`
 dockerPushLatest | optional | str | Specify if Jenkins should push the docker image with `latest` tag. <br /><br />**Default**: `true`
 dockerNexusRepo | optional | str | The name of the Docker Nexus repository where the project Docker image `dockerImageName` will be published to if `pushImage` is set.<br /><br />**Default**: `staging`
 pushImage | optional | bool | Specify if Jenkins should push your project's image to `dockerNexusRepo`.<br /><br />**Default**: `true`
 archiveImage | optional | bool | Specify if the built image need to be archived.<br /><br />**Default**: `false`
 archiveName | optional | bool | The name of the archived image.<br /><br />**Default**: `${_projectName}-archive.tar.gz`
 semverBump | optional | str | The semver axis to bump, see [git-semver](https://github.com/edgexfoundry/git-semver) for valid axis values.<br /><br />**Default**: `pre`
 releaseBranchOverride | optional | str | Specify if you want to override the release branch.
 failureNotify | optional | str | The group emails (comma-delimited) to email when the Jenkins job fails.<br /><br />**Default**: `edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org`
 arch | optional | array | A list of system architectures to target for the build. Possible values are `amd64` or `arm64`.<br /><br />**Default**: ['amd64', 'arm64']

 ## Usage

 ### Basic example

 ```groovy
 edgeXBuildDocker (
     project: 'docker-edgex-consul',
     dockerImageName: 'docker-edgex-consul',
     semver: true
 )
 ```

 ### Complex example

 ```groovy
 edgeXBuildDocker (
     project: 'edgex-compose',
     mavenSettings: 'ci-build-images-settings',
     dockerImageName: 'custom-edgex-compose',
     dockerNamespace: 'edgex-devops',
     dockerNexusRepo: 'snapshots',
     dockerTags: ["1.24.1"],
     releaseBranchOverride: 'edgex-compose'
 )
 ```

 ### Full example
 This example shows all the settings that can be specified and their default values.

 ```groovy
 edgeXBuildDocker (
     project: 'sample-project',
     mavenSettings: 'sample-project-settings',
     semver: true,
     dockerFilePath: 'Dockerfile',
     dockerBuildContext: '.',
     dockerBuildArgs: [],
     dockerNamespace: '',
     dockerImageName: 'docker-sample-project',
     dockerTags: [],
     dockerPushLatest: true,
     dockerNexusRepo: 'staging',
     pushImage: true,
     archiveImage: false,
     archiveName: sample-project-archive.tar.gz,
     semverBump: 'pre',
     releaseBranchOverride: 'golang',
     failureNotify: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
     arch: ['amd64', 'arm64']
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
        parameters {
            string(
                name: 'CommitId',
                defaultValue: '',
                description: 'The commitId in the code repository from where to initiate the build - should be used only if building via edgeXRelease')
        }
        stages {
            stage('Prepare') {
                steps {
                    script { edgex.releaseInfo() }
                    edgeXSetupEnvironment(_envVarMap)
                    // docker login for the to make sure all docker commands are authenticated
                    // in this specific node
                    edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
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
                                        if(params.CommitId) {
                                            sh "git checkout ${params.CommitId}"
                                        }
                                        // enableDockerProxy('https://nexus3.edgexfoundry.org:10001')
                                        // docker login for the to make sure all docker commands are authenticated
                                        // in this specific node
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
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
                                        if(params.CommitId) {
                                            sh "git checkout ${params.CommitId}"
                                        }
                                        // enableDockerProxy('https://nexus3.edgexfoundry.org:10001')
                                        // docker login for the to make sure all docker commands are authenticated
                                        // in this specific node
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
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

            // Comment this out in favor of Snyk scanning
            // When scanning the clair image, the FQDN is needed
            // stage('Clair Scan') {
            //     when {
            //         allOf {
            //             environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
            //             expression { edgex.isReleaseStream() || (env.GIT_BRANCH == env.RELEASE_BRANCH_OVERRIDE) }
            //         }
            //     }
            //     steps {
            //         script {
            //             if(edgex.nodeExists(config, 'amd64') && taggedAMD64Images) {
            //                 edgeXClair(taggedAMD64Images.first())
            //             }
            //             if(edgex.nodeExists(config, 'arm64') && taggedARM64Images) {
            //                 edgeXClair(taggedARM64Images.first())
            //             }
            //         }
            //     }
            // }

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
                script {
                    currentBuild.result = 'FAILED'
                    // only send email when on a release stream branch i.e. main, hanoi, ireland, etc.
                    if(edgex.isReleaseStream()) {
                        edgeXEmail(emailTo: env.BUILD_FAILURE_NOTIFY_LIST)
                    }
                }
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
    def _dockerImageName       = config.dockerImageName ?: _projectName
    def _dockerTags            = config.dockerTags ?: []
    def _dockerPushLatest      = edgex.defaultTrue(config.dockerPushLatest)
    def _dockerNexusRepo       = config.dockerNexusRepo ?: 'staging'
    def _pushImage             = edgex.defaultTrue(config.pushImage)
    def _archiveImage          = edgex.defaultFalse(config.archiveImage)
    def _archiveName           = config.archiveName ?: "${_projectName}-archive.tar.gz"
    def _semverBump            = config.semverBump ?: 'pre'
    def _releaseBranchOverride = config.releaseBranchOverride
    def _failureNotify       = config.failureNotify ?: 'edgex-tsc-devops@lists.edgexfoundry.org'

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
        SEMVER_BUMP_LEVEL: _semverBump,
        BUILD_FAILURE_NOTIFY_LIST: _failureNotify
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

// Temp fix while LF updates base packer images
def enableDockerProxy(proxyHost, debug = false) {
    sh "sudo jq \'. + {\"registry-mirrors\": [\"${proxyHost}\"], debug: ${debug}}\' /etc/docker/daemon.json > /tmp/daemon.json"
    sh 'sudo mv /tmp/daemon.json /etc/docker/daemon.json'
    sh 'sudo service docker restart | true'
}