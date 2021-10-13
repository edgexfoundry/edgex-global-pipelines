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

/**
 # edgeXBuildCApp

 Shared Library to build C projects

 ## Overview

 ![edgeXBuildCApp](images/edgeXBuildCApp.png)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 project | required | str | The name of your project. |
 mavenSettings | optional | str | The maven settings file in Jenkins that has been created for your project. **Note** the maven settings file specified must exist in Jenkins in order for your project to build.<br /><br />**Default**: `${project}-settings`
 semver | optional | bool | Specify if semantic versioning will be used to version your project. **Note** edgeX utilizes [git-semver](https://github.com/edgexfoundry/git-semver) for semantic versioning.<br /><br />**Default**: `true`
 testScript | optional | str | The command the build will use to test your project. **Note** the specified test script will execute in the project's CI build container.<br /><br />**Default**: `make test`
 buildScript | optional | str | The command the build will use to build your project.<br /><br />**Default**: `make build`
 dockerBaseImage | optional | str | The docker base image for your project.<br /><br />**Default**: `nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest`
 dockerFilePath | optional | str | The path to the Dockerfile for your project.<br /><br />**Default**: `Dockerfile`
 dockerBuildFilePath | optional | str | The path to the Dockerfile that will serve as the CI build image for your project.<br /><br />**Default**: `Dockerfile.build`
 dockerBuildContext | optional | str | The path for Docker to use as its build context when building your project. This applies to building both the CI build image and project image.<br /><br />**Default**: `.`
 dockerBuildImageTarget | optional | str | The name of the docker multi-stage-build stage the pipeline will use when building the CI build image.<br /><br />**Default**: `builder`
 dockerBuildArgs | optional | list | The list of additonal arguments to pass to Docker when building the image for your project.<br /><br />**Default**: `[]`
 dockerNamespace | optional | str | The docker registry namespace to use when publishing Docker images. **Note** for EdgeX projects images are published to the root of the docker registry and thus the namespace should be empty.<br /><br />**Default**: `''`
 dockerImageName | optional | str | The name of the Docker image for your project.<br /><br />**Default**: `docker-${project}`
 dockerNexusRepo | optional | str | The name of the Docker Nexus repository where the project Docker image `dockerImageName` will be published to if `pushImage` is set.<br /><br />**Default**: `staging`
 buildImage | optional | bool | Specify if Jenkins should build a Docker image for your project. **Note** if false then `pushImage` will also be set to false.<br /><br />**Default**: `true`
 pushImage | optional | bool | Specify if Jenkins should push your project's image to `dockerNexusRepo`.<br /><br />**Default**: `true`
 semverBump | optional | str | The semver axis to bump, see [git-semver](https://github.com/edgexfoundry/git-semver) for valid axis values.<br /><br />**Default**: `pre`
 buildSnap | optional | bool | Specify if Jenkins should build a Snap for your project. **Note** If set, your project must also include a valid snapcraft yaml `snap/snapcraft.yaml` for Jenkins to attempt to build the Snap.<br /><br />**Default**: `false`
 failureNotify | optional | str | The group emails (comma-delimited) to email when the Jenkins job fails.<br /><br />**Default**: `edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org`
 arch | optional | array | A list of system architectures to target for the build. Possible values are `amd64` or `arm64`.<br /><br />**Default**: ['amd64', 'arm64']

 ## Usage

 ### Basic example

 ```groovy
 edgeXBuildCApp (
    project: 'device-bacnet-c'
 )
 ```

 ### Complex example

 ```groovy
 edgeXBuildCApp (
     project: 'device-sdk-c',
     dockerBuildFilePath: 'scripts/Dockerfile.alpine-3.11-base',
     dockerFilePath: 'scripts/Dockerfile.alpine-3.11',
     testScript: 'apk add --update --no-cache openssl ca-certificates && make test',
     pushImage: false
 )
 ```

 ### Full example
 This example shows all the settings that can be specified and their default values.

 ```groovy
 edgeXBuildCApp (
     project: 'c-project',
     mavenSettings: 'c-project-settings',
     semver: true,
     testScript: 'make test',
     buildScript: 'make build',
     dockerBaseImage: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest',
     dockerFilePath: 'Dockerfile',
     dockerBuildFilePath: 'Dockerfile.build',
     dockerBuildContext: '.',
     dockerBuildArgs: [],
     dockerNamespace: '',
     dockerImageName: 'docker-c-project',
     dockerNexusRepo: 'staging',
     buildImage: true,
     pushImage: true,
     semverBump: 'pre',
     buildSnap: false,
     failureNotify: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
     arch: ['amd64', 'arm64']
 )
 ```
 */

def taggedAMD64Images
def taggedARM64Images

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
                    script {
                        edgex.releaseInfo()
                        edgeXSetupEnvironment(_envVarMap)
                        // docker login for the to make sure all docker commands are authenticated
                        // in this specific node
                        if(env.BUILD_DOCKER_IMAGE == 'true') {
                            edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                        }
                    }
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
                                        if(params.CommitId) {
                                            sh "git checkout ${params.CommitId}"
                                        }
                                        // docker login for the to make sure all docker commands are authenticated
                                        // in this specific node
                                        if(env.BUILD_DOCKER_IMAGE == 'true') {
                                            edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        }
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
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS) // TODO: this should not be needed anymore
                                        taggedAMD64Images = edgeXDocker.push("${env.DOCKER_IMAGE_NAME}", true, "${DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }

                            stage('Snap') {
                                agent {
                                    node {
                                        label 'ubuntu18.04-docker-8c-8g'
                                        customWorkspace "/w/workspace/${env.PROJECT}/${env.BUILD_ID}"
                                    }
                                }
                                when {
                                    beforeAgent true
                                    allOf {
                                        environment name: 'BUILD_SNAP', value: 'true'
                                        expression { findFiles(glob: 'snap/snapcraft.yaml').length == 1 }
                                        expression { !edgex.isReleaseStream() }
                                    }
                                }
                                steps {
                                    script {
                                        edgeXSnap()
                                    }
                                }
                            }
                        }
                        post {
                            always {
                                script { edgex.parallelJobCost() }
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
                                        edgex.patchAlpineSeccompArm64()

                                        if(params.CommitId) {
                                            sh "git checkout ${params.CommitId}"
                                        }
                                        // docker login for the to make sure all docker commands are authenticated
                                        // in this specific node
                                        if(env.BUILD_DOCKER_IMAGE == 'true') {
                                            edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        }
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
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS) // TODO: this should not be needed anymore
                                        taggedARM64Images = edgeXDocker.push("${env.DOCKER_IMAGE_NAME}-${env.ARCH}", true, "${env.DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }

                            // Turning off arm64 Snap stage Per WG meeting 10/29/20
                            /*stage('Snap') {
                                agent {
                                    node {
                                        label 'ubuntu18.04-docker-arm64-16c-16g'
                                        customWorkspace "/w/workspace/${env.PROJECT}/${env.BUILD_ID}"
                                    }
                                }
                                when {
                                    beforeAgent true
                                    allOf {
                                        environment name: 'BUILD_SNAP', value: 'true'
                                        expression { findFiles(glob: 'snap/snapcraft.yaml').length == 1 }
                                        expression { !edgex.isReleaseStream() }
                                    }
                                }
                                steps {
                                    script {
                                        edgeXSnap()
                                    }
                                }
                            }*/
                        }
                        post {
                            always {
                                script { edgex.parallelJobCost('arm64') }
                            }
                        }
                    }
                }
            }

            //////////////////////////////////////////////////////////////////
            // We should be back on the mainAgent here.

            // CodeCov should only run once, no need to run per arch only
            // TODO: Coverage report is required to run CodeCov.
            // Test/QA and Device services WG will need to define unit testing for C code.
            // stage('CodeCov') {
            //     when { environment name: 'SILO', value: 'production' }
            //     steps {
            //         unstash 'coverage-report'
            //         edgeXCodecov "${env.PROJECT}-codecov-token"
            //     }
            // }
            // Comment this out in favor of Snyk scanning
            // // When scanning the clair image, the FQDN is needed
            // stage('Clair Scan') {
            //     when {
            //         allOf {
            //             environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
            //             environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
            //             expression { edgex.isReleaseStream() }
            //         }
            //     }
            //     steps {
            //         script {
            //             if(edgex.nodeExists(config, 'amd64')) {
            //                 def amd64Image = edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}")
            //                 edgeXClair("${DOCKER_REGISTRY}/${amd64Image}:${GIT_COMMIT}")
            //             }
            //             if(edgex.nodeExists(config, 'arm64')) {
            //                 def arm64Image = edgeXDocker.finalImageName("${DOCKER_IMAGE_NAME}-arm64")
            //                 edgeXClair("${DOCKER_REGISTRY}/${arm64Image}:${GIT_COMMIT}")
            //             }
            //         }
            //     }
            // }

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

    // If the Dockerfile.build exits? use it
    if(fileExists(env.DOCKER_BUILD_FILE_PATH)) {
        def buildArgString = buildArgs.join(' --build-arg ')

        docker.build(
            "ci-base-image-${env.ARCH}",
            "-f ${env.DOCKER_BUILD_FILE_PATH} ${buildArgString} ${env.DOCKER_BUILD_CONTEXT}"
        )
    } else {
        if(env.DOCKER_FILE_PATH && fileExists(env.DOCKER_FILE_PATH)) {
            buildArgs << 'MAKE="echo noop"'
            def buildArgString = buildArgs.join(' --build-arg ')

            docker.build(
                "ci-base-image-${env.ARCH}",
                "-f ${env.DOCKER_FILE_PATH} ${buildArgString} --target=${env.DOCKER_BUILD_IMAGE_TARGET} ${env.DOCKER_BUILD_CONTEXT}"
            )
        } else {
            // just retag the base image if no Dockerfile exists in the repo
            sh "docker pull ${baseImage}"
            sh "docker tag ${baseImage} ci-base-image-${env.ARCH}"
        }
    }
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
    def _dockerBuildImageTarget = config.dockerBuildImageTarget ?: 'builder'
    def _dockerBuildArgs     = config.dockerBuildArgs ?: []
    def _dockerNamespace     = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerImageName     = config.dockerImageName ?: _projectName.replaceAll(/-c$/, '')
    def _dockerNexusRepo     = config.dockerNexusRepo ?: 'staging'
    def _buildImage          = edgex.defaultTrue(config.buildImage)
    def _pushImage           = edgex.defaultTrue(config.pushImage)
    def _semverBump          = config.semverBump ?: 'pre'
    //def _snapChannel         = config.snapChannel ?: 'latest/edge'
    def _buildSnap           = edgex.defaultFalse(config.buildSnap)
    def _failureNotify       = config.failureNotify ?: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org'

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
        DOCKER_BUILD_IMAGE_TARGET: _dockerBuildImageTarget,
        DOCKER_IMAGE_NAME: _dockerImageName,
        DOCKER_REGISTRY_NAMESPACE: _dockerNamespace,
        DOCKER_NEXUS_REPO: _dockerNexusRepo,
        BUILD_DOCKER_IMAGE: _buildImage,
        PUSH_DOCKER_IMAGE: _pushImage,
        SEMVER_BUMP_LEVEL: _semverBump,
        //SNAP_CHANNEL: _snapChannel,
        BUILD_SNAP: _buildSnap,
        BUILD_FAILURE_NOTIFY_LIST: _failureNotify
    ]

    // encode with comma in case build arg has space
    if(_dockerBuildArgs) {
        envMap << [ DOCKER_BUILD_ARGS: _dockerBuildArgs.join(',')]
    }

    edgex.bannerMessage "[edgeXBuildCApp] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}