import org.jenkinsci.plugins.workflow.libs.Library
//
// Copyright (c) 2019-2021 Intel Corporation
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

/**
 # edgeXBuildGoParallel

 Shared Library to build Go projects and Docker images in parallel. Utilizes docker-compose --parallel to build Docker images found in the workspace. Currently only used for the **edgex-go** mono-repo.

 ## Overview

 ![edgeXBuildGoApp](images/edgeXBuildGoParallel.png)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 project | required | str | The name of your project. |
 mavenSettings | optional | str | The maven settings file in Jenkins that has been created for your project. **Note** the maven settings file specified must exist in Jenkins in order for your project to build.<br /><br />**Default**: `${project}-settings`
 semver | optional | bool | Specify if semantic versioning will be used to version your project. **Note** edgeX utilizes [git-semver](https://github.com/edgexfoundry/git-semver) for semantic versioning.<br /><br />**Default**: `true`
 testScript | optional | str | The command the build will use to test your project. **Note** the specified test script will execute in the project's CI build container.<br /><br />**Default**: `make test`
 buildScript | optional | str | The command the build will use to build your project.<br /><br />**Default**: `make build`
 goVersion | optional | str | The version of Go to use for building the project's CI build image. **Note** this parameter is used in conjuction with the `useAlpineBase` parameter to determine the base for the project's CI build image.<br /><br />**Default**: `1.18`
 goProxy | optional | str | The proxy to use when downloading Go modules. The value of this parameter will be set in the `GOPROXY` environment variable to control the download source of Go modules.<br /><br />**Default**: `https://nexus3.edgexfoundry.org/repository/go-proxy/`
 useAlpineBase | optional | bool | Specify if an Alpine-based `edgex-golang-base:${goVersion}-alpine` image will be used as the base for the project's CI build image. If true, the respective `edgex-golang-base` image should exist in the Nexus snapshot repository, if a matching image is not found in Nexus then an Alpine-based `go-lang:${goVersion}-alpine` DockerHub image will be used. If false, then a non-Alpine `go-lang:${goVersion}` DockerHub image will be used. **Note** this parameter is used in conjuction with the `goVersion` parameter to determine the base for the projects' CI build image.<br /><br />**Default**: `true`
 dockerFileGlobPath | optional | str | The pattern for finding Dockerfiles to build. **Note** Docker images will be named with the same name as the directory which the Dockerfile was found in with a `docker-` prefix and `-go` suffix. Example: `docker-<folder>-go`<br /><br />**Default**: `cmd/** /Dockerfile`
 dockerImageNamePrefix | optional | str | The prefix to apply to the names of all the Docker images built.<br /><br />**Default**: `docker-`
 dockerImageNameSuffix | optional | str | The suffix to apply to the names of all the Docker images built.<br /><br />**Default**: `-go`
 dockerBuildFilePath | optional | str | The path to the Dockerfile that will serve as the CI build image for your project.<br /><br />**Default**: `Dockerfile.build`
 dockerBuildContext | optional | str | The path for Docker to use as its build context when building your project. This applies to building both the CI build image and project image.<br /><br />**Default**: `.`
 dockerBuildImageTarget | optional | str | The name of the docker multi-stage-build stage the pipeline will use when building the CI build image.<br /><br />**Default**: `builder`
 dockerNamespace | optional | str | The docker registry namespace to use when publishing Docker images. **Note** for EdgeX projects images are published to the root of the docker registry and thus the namespace should be empty.<br /><br />**Default**: `''`
 dockerNexusRepo | optional | str | The name of the Docker Nexus repository where the project Docker images will be published to if `pushImage` is set.<br /><br />**Default**: `staging`
 buildImage | optional | bool | Specify if Jenkins should build a Docker image for your project. **Note** if false then `pushImage` will also be set to false<br /><br />**Default**: `true`
 pushImage | optional | bool | Specify if Jenkins should push your project's image to `dockerNexusRepo`.<br /><br />**Default**: `true`
 semverBump | optional | str | The semver axis to bump, see [git-semver](https://github.com/edgexfoundry/git-semver) for valid axis values.<br /><br />**Default**: `pre`
 buildSnap | optional | bool | Specify if Jenkins should build a Snap for your project. **Note** If set, your project must also include a valid snapcraft yaml `snap/snapcraft.yaml` for Jenkins to attempt to build the Snap.<br /><br />**Default**: `false`
 publishSwaggerDocs | optional | bool | Specify if Jenkins should attempt to publish your projects API documentation to SwaggerHub. **Note** in order for Jenkins to publish to SwaggerHub you must ensure a valid value for `swaggerApiFolders` is set.<br /><br />**Default**: `false`
 swaggerApiFolders | optional | list | The list of paths to your projects API Swagger-based documentation.<br /><br />**Default**: `['openapi/v1', 'openapi/v2']`
 failureNotify | optional | str | The group emails (comma-delimited) to email when the Jenkins job fails.<br /><br />**Default**: `edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org`
 arch | optional | array | A list of system architectures to target for the build. Possible values are `amd64` or `arm64`.<br /><br />**Default**: ['amd64', 'arm64']

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

 ### Full example
 This example shows all the settings that can be specified and their default values.

 ```groovy
 edgeXBuildGoParallel (
     project: 'go-project',
     mavenSettings: 'go-project-settings',
     semver: true,
     testScript: 'make test',
     buildScript: 'make build',
     goVersion: '1.16',
     goProxy: 'https://nexus3.edgexfoundry.org/repository/go-proxy/',
     useAlpineBase: true,
     dockerFileGlobPath: 'cmd/** /Dockerfile',
     dockerImageNamePrefix: 'docker-',
     dockerImageNameSuffix: '-go',
     dockerBuildFilePath: 'Dockerfile.build',
     dockerBuildContext: '.',
     dockerNamespace: '',
     dockerNexusRepo: 'staging',
     buildImage: true,
     pushImage: true,
     semverBump: 'pre',
     buildSnap: false,
     publishSwaggerDocs: false,
     swaggerApiFolders: ['openapi/v1', 'openapi/v2'],
     failureNotify: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
     arch: ['amd64', 'arm64']
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
            disableConcurrentBuilds()
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

                        // Generate list docker images to build
                        dockerImagesToBuild = getDockersFromFilesystem(env.DOCKER_FILE_GLOB, env.DOCKER_IMAGE_NAME_PREFIX, env.DOCKER_IMAGE_NAME_SUFFIX)
                    }
                }
            }

            stage('Build Check') {
                when {
                    expression { !edgex.isLTSReleaseBuild() }
                }
                stages {
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
                                                if(params.CommitId) {
                                                    sh "git checkout ${params.CommitId}"
                                                }
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
                                                edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS) // TODO: this should not be needed anymore
                                                taggedAMD64Images = edgeXDocker.pushAll(dockerImagesToBuild, !edgex.isLTS(), env.DOCKER_NEXUS_REPO, env.ARCH)
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
                                            edgeXSnap()
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
                                                edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS) // TODO: this should not be needed anymore
                                                taggedARM64Images = edgeXDocker.pushAll(dockerImagesToBuild, !edgex.isLTS(), env.DOCKER_NEXUS_REPO, env.ARCH)
                                            }
                                        }
                                    }

                                    // Turning off arm64 Snap stage Per WG meeting 08/27/20
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
                                            edgeXSnap()
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
                            edgeXSnyk(projectName: "edgexfoundry/${env.PROJECT}:${env.GIT_BRANCH}")
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

def testAndVerify(codeCov = true) {
    edgex.bannerMessage "[edgeXBuildGoParallel] Running Tests and Build..."

    // fixes permissions issues due new Go 1.18 buildvcs checks
    sh 'git config --global --add safe.directory $WORKSPACE'

    // TODO: This should go away after Kamakura, all repos now have a go.sum file.
    if(!fileExists('go.sum') && env.GO_VERSION =~ '1.16') {
        sh 'go mod tidy' // for Go 1.16
    }
    sh env.TEST_SCRIPT

    // deal with changes that can happen to the go.mod file when dealing with vendored dependencies
    if(edgex.isLTS() && fileExists('vendor')) {
        sh 'go mod tidy'
    }

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
    sh 'git config --global --add safe.directory $WORKSPACE'

    // this would be something like golang:1.13 or a pre-built devops managed image from ci-build-images
    def goVersion = edgex.isLTS() ? edgex.getGoModVersion() : env.GO_VERSION
    def baseImage = edgex.getGoLangBaseImage(goVersion, env.USE_ALPINE)

    if(env.ARCH == 'arm64' && baseImage.contains(env.DOCKER_REGISTRY)) {
        baseImage = baseImage.replace('edgex-golang-base', "edgex-golang-base-${env.ARCH}")
    }

    edgex.bannerMessage "[edgeXBuildGoParallel] Building Code With image [${baseImage}]"

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
            sh "docker pull ${baseImage}"

            // This will build a new ci-base-image with all the Go modules enabled.
            // This could externalized to another file, but for now due to simplicity, I am doing the build inline
            if(fileExists('go.mod')) {
                sh "echo \"FROM ${baseImage}\nWORKDIR /edgex\nCOPY go.mod .\nRUN go mod download\" | docker build -t ci-base-image-${env.ARCH} -f - ."
            } else {
                sh "docker tag ${baseImage} ci-base-image-${env.ARCH}"
            }
        }
    }
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
    def _goVersion       = config.goVersion ?: '1.18'
    def _useAlpine       = edgex.defaultTrue(config.useAlpineBase)

    def _dockerFileGlob        = config.dockerFileGlobPath ?: 'cmd/**/Dockerfile'
    def _dockerImageNamePrefix = config.dockerImageNamePrefix ?: '' //'docker-'
    def _dockerImageNameSuffix = config.dockerImageNameSuffix ?: '' //'-go'
    def _dockerBuildFilePath   = config.dockerBuildFilePath ?: 'Dockerfile.build'
    def _dockerBuildContext    = config.dockerBuildContext ?: '.'
    def _dockerBuildImageTarget = config.dockerBuildImageTarget ?: 'builder'
    def _dockerNamespace       = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerNexusRepo       = config.dockerNexusRepo ?: 'staging'
    def _buildImage            = edgex.defaultTrue(config.buildImage)
    def _pushImage             = edgex.defaultTrue(config.pushImage)
    def _semverBump            = config.semverBump ?: 'pre'
    def _goProxy               = config.goProxy ?: 'https://nexus3.edgexfoundry.org/repository/go-proxy/'
    def _publishSwaggerDocs    = edgex.defaultFalse(config.publishSwaggerDocs)
    def _swaggerApiFolders     = config.swaggerApiFolders ?: ['openapi/v1', 'openapi/v2']
    def _failureNotify         = config.failureNotify ?: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org'
    def _snykDebug             = edgex.defaultFalse(config.snykDebug)

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
        USE_ALPINE: _useAlpine,
        DOCKER_FILE_GLOB: _dockerFileGlob,
        DOCKER_IMAGE_NAME_PREFIX: _dockerImageNamePrefix,
        DOCKER_IMAGE_NAME_SUFFIX: _dockerImageNameSuffix,
        DOCKER_BUILD_FILE_PATH: _dockerBuildFilePath,
        DOCKER_BUILD_CONTEXT: _dockerBuildContext,
        DOCKER_BUILD_IMAGE_TARGET: _dockerBuildImageTarget,
        DOCKER_REGISTRY_NAMESPACE: _dockerNamespace,
        DOCKER_NEXUS_REPO: _dockerNexusRepo,
        BUILD_DOCKER_IMAGE: _buildImage,
        PUSH_DOCKER_IMAGE: _pushImage,
        SEMVER_BUMP_LEVEL: _semverBump,
        //GOPROXY: _goProxy,
        PUBLISH_SWAGGER_DOCS: _publishSwaggerDocs,
        SWAGGER_API_FOLDERS: _swaggerApiFolders.join(' '),
        // SNAP_CHANNEL: _snapChannel,
        BUILD_SNAP: _buildSnap,
        BUILD_FAILURE_NOTIFY_LIST: _failureNotify,
        SNYK_DEBUG: _snykDebug
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
