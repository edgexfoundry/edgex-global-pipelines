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
 #edgeXBuildGoParallel
 
 Shared Library to build Go projects and Docker images in parallel. Utilizes docker-compose --parallel to build Docker images found in the workspace. Currently only used for the **edgex-go** mono-repo.
 
 ## Parameters

 * **project** - **Required** Specify your project name
 * **dockerFileGlobPath** - **Required** Pattern for finding Dockerfiles to build. Note docker images will be named with the same name as the directory which the Dockerfile was found in with a docker- prefix and -go suffix. Example: `docker-<folder>-go`
 * more coming soon...

 ## Usage
 
 ### Basic example

 ```groovy
 edgeXBuildGoParallel (
     project: 'edgex-go',
     dockerFileGlobPath: 'cmd/** /Dockerfile',
 )
 ```
 
 ### Complex example
 
 ```groovy
 edgeXBuildGoParallel(
    project: 'edgex-go',
    dockerFileGlobPath: 'cmd/** /Dockerfile',
    testScript: 'make test',
    buildScript: 'make build',
    publishSwaggerDocs: true,
    swaggerApiFolders: ['openapi/v1', 'openapi/v2'],
    buildSnap: true
 )
 ```
 */

def taggedAMD64Images = []
def taggedARM64Images = []
def dockerImagesToBuild

def call(config) {
    edgex.bannerMessage "[edgeXBuildGoParallel] RAW Config: ${config}"

    validate(config)
    edgex.setupNodes(config)

    def _envVarMap = toEnvironment(config)

    ///////////////////////////////////////////////////////////////////////////

    pipeline {
        agent {
            node {
                label edgex.mainNode(config)
                customWorkspace "/w/workspace/${config.project}/${env.BUILD_ID}"
            }
        }
        options {
            timestamps()
            preserveStashes()
            quietPeriod(5)
            durabilityHint 'PERFORMANCE_OPTIMIZED'
            timeout(360)
        }
        triggers {
            issueCommentTrigger('.*^recheck$.*')
        }

        stages {
            stage('Prepare') {
                steps {
                    script {
                        edgex.releaseInfo()
                        edgeXSetupEnvironment(_envVarMap)
                        // docker login for the to make sure all docker commands are authenticated
                        // in this specific node
                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)

                        // Generate list docker images to build
                        dockerImagesToBuild = getDockersFromFilesystem(env.DOCKER_FILE_GLOB, env.DOCKER_IMAGE_NAME_PREFIX, env.DOCKER_IMAGE_NAME_SUFFIX)
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
                        // should be running on the initial "mainNode"
                        when {
                            beforeAgent true
                            expression { edgex.nodeExists(config, 'amd64') }
                        }
                        environment {
                            ARCH = 'x86_64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        enableDockerProxy('https://nexus3.edgexfoundry.org:10001')
                                        // builds ci-base-image used to cache dependencies and system libs
                                        prepBaseBuildImage()
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') { sh 'go version' }
                                    }
                                }
                            }

                            stage('Test') {
                                // need to always run this stage due to codecov always needing the coverage.out file
                                // when {
                                //     expression { !edgex.isReleaseStream() }
                                // }
                                steps {
                                    script {
                                        // docker.sock bind mount needed due to `make raml_verify` launching a docker image
                                        // docker: Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: 
                                        docker.image("ci-base-image-${env.ARCH}")
                                            .inside('-u 0:0 -v /var/run/docker.sock:/var/run/docker.sock --privileged')
                                        {
                                            testAndVerify()
                                        }
                                    }
                                }
                            }

                            stage('Docker Build') {
                                when { environment name: 'BUILD_DOCKER_IMAGE', value: 'true' }
                                steps {
                                    script {
                                        edgeXDocker.buildInParallel(dockerImagesToBuild, null, "ci-base-image-${env.ARCH}")
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
                                        taggedAMD64Images = edgeXDocker.pushAll(dockerImagesToBuild, false, env.DOCKER_NEXUS_REPO, env.ARCH)
                                    }
                                }
                            }

                            stage('Snap') {
                                agent {
                                    node {
                                        label 'centos7-docker-8c-8g'
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
                                    edgeXSnap(jobType: 'build')
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
                                customWorkspace "/w/workspace/${config.project}/${env.BUILD_ID}"
                            }
                        }
                        environment {
                            ARCH = 'arm64'
                        }
                        stages {
                            stage('Prep') {
                                steps {
                                    script {
                                        enableDockerProxy('https://nexus3.edgexfoundry.org:10001')
                                        // docker login for the to make sure all docker commands are authenticated
                                        // in this specific node
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        if(env.USE_SEMVER == 'true') {
                                            unstash 'semver'
                                        }
                                        prepBaseBuildImage()
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') { sh 'go version' }
                                    }
                                }
                            }

                            stage('Test') {
                                when {
                                    expression { !edgex.isReleaseStream() }
                                }
                                steps {
                                    script {
                                        // docker.sock bind mount needed due to `make raml_verify` launching a docker image
                                        //docker: Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: 
                                        docker.image("ci-base-image-${env.ARCH}")
                                            .inside('-u 0:0 -v /var/run/docker.sock:/var/run/docker.sock --privileged')
                                        {
                                            testAndVerify()
                                        }
                                    }
                                }
                            }

                            stage('Docker Build') {
                                when { environment name: 'BUILD_DOCKER_IMAGE', value: 'true' }
                                steps {
                                    script {
                                        edgeXDocker.buildInParallel(dockerImagesToBuild, null, "ci-base-image-${env.ARCH}")
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
                                        taggedARM64Images = edgeXDocker.pushAll(dockerImagesToBuild, false, env.DOCKER_NEXUS_REPO, env.ARCH)
                                    }
                                }
                            }

                            // Turning off arm64 Snap stage Per WG meeting 08/27/20
                            // stage('Snap') {
                            //     agent {
                            //         node {
                            //             label 'ubuntu18.04-docker-arm64-16c-16g'
                            //             customWorkspace "/w/workspace/${env.PROJECT}/${env.BUILD_ID}"
                            //         }
                            //     }
                            //     when {
                            //         beforeAgent true
                            //         allOf {
                            //             environment name: 'BUILD_SNAP', value: 'true'
                            //             expression { findFiles(glob: 'snap/snapcraft.yaml').length == 1 }
                            //             expression { !edgex.isReleaseStream() }
                            //         }
                            //     }
                            //     steps {
                            //         edgeXSnap(jobType: 'build')
                            //     }
                            // }
                        }
                    }
                }
            }

            //////////////////////////////////////////////////////////////////
            // We should be back on the mainAgent here.

            // CodeCov should only run once during each PR build
            stage('CodeCov') {
                when {
                    allOf {
                        environment name: 'SILO', value: 'production'
                        // expression { !edgex.isReleaseStream() } // always run the codecov scan
                    }
                }
                steps {
                    unstash 'coverage-report'
                    edgeXCodecov "${env.PROJECT}-codecov-token"
                }
            }

            // Scan Go Dependencies
            stage('Snyk Scan') {
                when { expression { edgex.isReleaseStream() } }
                steps {
                    edgeXSnyk()
                }
            }

            stage('Snyk Docker Image Scan') {
                when { 
                    allOf {
                        environment name: 'BUILD_DOCKER_IMAGE', value: 'true'
                        environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
                        expression { edgex.isReleaseStream() }
                    }
                }
                steps {
                    script {
                        if(edgex.nodeExists(config, 'amd64') && taggedAMD64Images) {
                            def tagged = getDockerfilesFromTagged(taggedAMD64Images, dockerImagesToBuild)
                            if(tagged) {
                                tagged.each {
                                    edgeXSnyk(
                                        command: 'test',
                                        dockerImage: it[0],
                                        dockerFile: it[1],
                                        severity: 'high',
                                        sendEmail: true,
                                        emailTo: env.SECURITY_NOTIFY_LIST,
                                        htmlReport: true
                                    )
                                }
                            }
                        }

                        // While ARM64 images can be scanned, this would double the amount of tests run
                        // so we are disabling arm64 scans for now
                        // if(edgex.nodeExists(config, 'arm64') && taggedARM64Images) {
                        //     def tagged = getDockerfilesFromTagged(taggedARM64Images, dockerImagesToBuild)
                        //     if(tagged) {
                        //         tagged.each {
                        //             edgeXSnyk(
                        //                 command: 'test',
                        //                 dockerImage: it[0],
                        //                 dockerFile: it[1],
                        //                 severity: 'high',
                        //                 sendEmail: true,
                        //                 emailTo: env.SECURITY_NOTIFY_LIST,
                        //                 htmlReport: true
                        //             )
                        //         }
                        //     }
                        // }
                    }
                }
            }

            stage('Publish Swagger') {
                when {
                    allOf {
                        environment name: 'PUBLISH_SWAGGER_DOCS', value: 'true'
                        expression { edgex.isReleaseStream() }
                    }
                }
                steps {
                    edgeXSwaggerPublish(apiFolders: env.SWAGGER_API_FOLDERS)
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

def testAndVerify(codeCov = true) {
    edgex.bannerMessage "[edgeXBuildGoParallel] Running Tests and Build..."

    // make test raml_verify
    sh env.TEST_SCRIPT

    if(codeCov) {
        sh 'ls -al .'
        // need to fix perms of coverage.out since this runs in a container as user 0
        sh '[ -e "coverage.out" ] && chown 1001:1001 coverage.out'
        stash name: 'coverage-report', includes: '**/*coverage.out', useDefaultExcludes: false, allowEmpty: true
    }

    // carry over from edgex-go, where they used to go build to verify all the
    // code before the docker images are built

    // make build
    sh env.BUILD_SCRIPT
}

def prepBaseBuildImage() {
    // this would be something like golang:1.13 or a pre-built devops managed image from ci-build-images
    def baseImage = env.DOCKER_BASE_IMAGE

    if(env.ARCH == 'arm64' && baseImage.contains(env.DOCKER_REGISTRY)) {
        baseImage = "${env.DOCKER_BASE_IMAGE}".replace('edgex-golang-base', "edgex-golang-base-${env.ARCH}")
    }

    edgex.bannerMessage "[edgeXBuildGoParallel] Building Code With image [${baseImage}]"

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
        error('[edgeXBuildGoParallel] The parameter "project" is required. This is typically the project name.')
    }
}

def toEnvironment(config) {
    def _projectName   = config.project
    def _mavenSettings = config.mavenSettings ?: "${_projectName}-settings"

    if(env.SILO == 'sandbox') {
        _mavenSettings = 'sandbox-settings'
    }

    def _useSemver       = edgex.defaultTrue(config.semver)
    def _testScript      = config.testScript ?: 'make test'
    def _buildScript     = config.buildScript ?: 'make build'
    def _goVersion       = config.goVersion ?: '1.15'
    def _useAlpine       = edgex.defaultTrue(config.useAlpineBase)
    def _dockerBaseImage = edgex.getGoLangBaseImage(_goVersion, _useAlpine)

    def _dockerFileGlob        = config.dockerFileGlobPath ?: 'cmd/**/Dockerfile'
    def _dockerImageNamePrefix = config.dockerImageNamePrefix ?: "docker-"
    def _dockerImageNameSuffix = config.dockerImageNameSuffix ?: "-go"
    def _dockerBuildFilePath   = config.dockerBuildFilePath ?: 'Dockerfile.build'
    def _dockerBuildContext    = config.dockerBuildContext ?: '.'
    def _dockerNamespace       = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerNexusRepo       = config.dockerNexusRepo ?: 'staging'
    def _buildImage            = edgex.defaultTrue(config.buildImage)
    def _pushImage             = edgex.defaultTrue(config.pushImage)
    def _semverBump            = config.semverBump ?: 'pre'
    def _goProxy               = config.goProxy ?: 'https://nexus3.edgexfoundry.org/repository/go-proxy/'
    def _publishSwaggerDocs    = edgex.defaultFalse(config.publishSwaggerDocs)
    def _swaggerApiFolders     = config.swaggerApiFolders ?: ['openapi/v1', 'openapi/v2']
    def _securityNotify        = 'security-issues@lists.edgexfoundry.org'

    // def _snapChannel           = config.snapChannel ?: 'latest/edge'
    def _buildSnap             = edgex.defaultFalse(config.buildSnap)

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
        GO_VERSION: _goVersion,
        DOCKER_BASE_IMAGE: _dockerBaseImage,
        DOCKER_FILE_GLOB: _dockerFileGlob,
        DOCKER_IMAGE_NAME_PREFIX: _dockerImageNamePrefix,
        DOCKER_IMAGE_NAME_SUFFIX: _dockerImageNameSuffix,
        DOCKER_BUILD_FILE_PATH: _dockerBuildFilePath,
        DOCKER_BUILD_CONTEXT: _dockerBuildContext,
        DOCKER_REGISTRY_NAMESPACE: _dockerNamespace,
        DOCKER_NEXUS_REPO: _dockerNexusRepo,
        BUILD_DOCKER_IMAGE: _buildImage,
        PUSH_DOCKER_IMAGE: _pushImage,
        SEMVER_BUMP_LEVEL: _semverBump,
        GOPROXY: _goProxy,
        PUBLISH_SWAGGER_DOCS: _publishSwaggerDocs,
        SWAGGER_API_FOLDERS: _swaggerApiFolders.join(' '),
        // SNAP_CHANNEL: _snapChannel,
        BUILD_SNAP: _buildSnap,
        SECURITY_NOTIFY_LIST: _securityNotify
    ]

    edgex.bannerMessage "[edgeXBuildGoParallel] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}

def getDockersFromFilesystem(dockerFileGlob, imageNamePrefix, imageNameSuffix) {
    def dockerFiles = sh(script: "for file in `ls ${dockerFileGlob}`; do echo \"\$(dirname \"\$file\" | cut -d/ -f2),\${file}\"; done", returnStdout: true).trim()

    def dockers = dockerFiles.split('\n').collect {
        def imageSplit  = it.split(",")
        def serviceName = imageSplit[0]
        def dockerFile  = imageSplit[1]
        [ image: "${imageNamePrefix}${serviceName}${imageNameSuffix}", dockerfile: dockerFile ]
    }

    println "Generate Dockers from filesystem: ${dockers}"

    dockers
}

def getDockerfilesFromTagged(tagged, dockers) {
    if(tagged) {
        tagged.collect {
            def imageName = it.split('/')[1]
            [it, dockers.find { imgSpec -> imageName =~ imgSpec.image }.dockerfile]
        }
    }
}

// Temp fix while LF updates base packer images
def enableDockerProxy(proxyHost, debug = false) {
    sh "sudo jq \'. + {\"registry-mirrors\": [\"${proxyHost}\"], debug: ${debug}}\' /etc/docker/daemon.json > /tmp/daemon.json"
    sh 'sudo mv /tmp/daemon.json /etc/docker/daemon.json'
    sh 'sudo service docker restart | true'
}