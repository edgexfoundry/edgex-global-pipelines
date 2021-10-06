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
 # edgeXBuildGoApp

 Shared Library to build Go projects

 ## Overview

 ![edgeXBuildGoApp](images/edgeXBuildGoApp.png)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 project | required | str | The name of your project. |
 mavenSettings | optional | str | The maven settings file in Jenkins that has been created for your project. **Note** the maven settings file specified must exist in Jenkins in order for your project to build.<br /><br />**Default**: `${project}-settings`
 semver | optional | bool | Specify if semantic versioning will be used to version your project. **Note** edgeX utilizes [git-semver](https://github.com/edgexfoundry/git-semver) for semantic versioning.<br /><br />**Default**: `true`
 testScript | optional | str | The command the build will use to test your project. **Note** the specified test script will execute in the project's CI build container.<br /><br />**Default**: `make test`
 buildScript | optional | str | The command the build will use to build your project.<br /><br />**Default**: `make build`
 goVersion | optional | str | The version of Go to use for building the project's CI build image. **Note** this parameter is used in conjuction with the `useAlpineBase` parameter to determine the base for the project's CI build image.<br /><br />**Default**: `1.16`
 goProxy | optional | str | The proxy to use when downloading Go modules. The value of this parameter will be set in the `GOPROXY` environment variable to control the download source of Go modules.<br /><br />**Default**: `https://nexus3.edgexfoundry.org/repository/go-proxy/`
 useAlpineBase | optional | bool | Specify if an Alpine-based `edgex-golang-base:${goVersion}-alpine` image will be used as the base for the project's CI build image. If true, the respective `edgex-golang-base` image should exist in the Nexus snapshot repository, if a matching image is not found in Nexus then an Alpine-based `go-lang:${goVersion}-alpine` DockerHub image will be used. If false, then a non-Alpine `go-lang:${goVersion}` DockerHub image will be used. **Note** this parameter is used in conjuction with the `goVersion` parameter to determine the base for the projects' CI build image.<br /><br />**Default**: `true`
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
 semverVersion | optional | str | This parameter isn't currently used and will be removed in a future version.<br /><br />**Default**: `''`
 buildSnap | optional | bool | Specify if Jenkins should build a Snap for your project. **Note** If set, your project must also include a valid snapcraft yaml `snap/snapcraft.yaml` for Jenkins to attempt to build the Snap.<br /><br />**Default**: `false`
 publishSwaggerDocs | optional | bool | Specify if Jenkins should attempt to publish your projects API documentation to SwaggerHub. **Note** in order for Jenkins to publish to SwaggerHub you must ensure a valid value for `swaggerApiFolders` is set.<br /><br />**Default**: `false`
 swaggerApiFolders | optional | list | The list of paths to your projects API Swagger-based documentation.<br /><br />**Default**: `['openapi/v1']`
 failureNotify | optional | str | The group emails (comma-delimited) to email when the Jenkins job fails.<br /><br />**Default**: `edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org`
 buildExperimentalDockerImage | optional | bool | Specify if Jenkins should add an additonal GitHub tag called `experimental` at the same commit where the semantic version is tagged. **Note** this feature is currently only used internally for DevOps builds.<br /><br />**Default**: `false`
 artifactTypes | optional | list | A list of types that the Jenkins build will designate as artifacts, valid list values are `docker` and `archive`. **Note** if `archive` is specified then all `tar.gz` or `zip` files that your project build creates in the `artifactRoot` folder will be archived to Nexus.<br /><br />**Default**: `['docker']`
 artifactRoot | optional | str | The path in the Jenkins workspace to designate as the artifact root folder. **Note** all files written to this directory within your build will be automatically pushed to Nexus when the Jenkins job completes.<br /><br />**Default**: `archives/bin`
 arch | optional | array | A list of system architectures to target for the build. Possible values are `amd64` or `arm64`.<br /><br />**Default**: ['amd64', 'arm64']

 ## Usage

 ### Basic example

 ```groovy
 edgeXBuildGoApp (
     project: 'device-random-go',
     goVersion: '1.16'
 )
 ```

 ### Complex example

 ```groovy
 edgeXBuildGoApp (
     project: 'app-functions-sdk-go',
     semver: true,
     goVersion: '1.16',
     testScript: 'make test',
     buildImage: false,
     publishSwaggerDocs: true,
     swaggerApiFolders: ['openapi/v2']
 )
 ```

 ### Full example
 This example shows all the settings that can be specified and their default values.

 ```groovy
 edgeXBuildGoApp (
     project: 'go-project',
     mavenSettings: 'go-project-settings',
     semver: true,
     testScript: 'make test',
     buildScript: 'make build',
     goVersion: '1.16',
     goProxy: 'https://nexus3.edgexfoundry.org/repository/go-proxy/',
     useAlpineBase: true,
     dockerFilePath: 'Dockerfile',
     dockerBuildFilePath: 'Dockerfile.build',
     dockerBuildContext: '.',
     dockerBuildArgs: [],
     dockerNamespace: '',
     dockerImageName: 'docker-go-project',
     dockerNexusRepo: 'staging',
     buildImage: true,
     pushImage: true,
     semverBump: 'pre',
     buildSnap: false,
     publishSwaggerDocs: false,
     swaggerApiFolders: ['openapi/v1'],
     failureNotify: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
     buildExperimentalDockerImage: false,
     artifactTypes: ['docker'],
     artifactRoot: 'archives/bin',
     arch: ['amd64', 'arm64']

 )
 ```
*/

def taggedAMD64Images
def taggedARM64Images

def call(config) {
    edgex.bannerMessage "[edgeXBuildGoApp] RAW Config: ${config}"

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
                    script {
                        // TODO: Refactor this code out into another function
                        def _commitMsg = edgex.getCommitMessage(env.GIT_COMMIT)
                        echo("GIT_COMMIT: ${env.GIT_COMMIT}, Commit Message: ${_commitMsg}")
                        def _buildVersion = ''
                        def _namedTag = ''
                        def _parsedCommitMsg = [:]

                        if(edgex.isBuildCommit(_commitMsg)) {
                            _parsedCommitMsg = edgex.parseBuildCommit(_commitMsg)
                            _buildVersion = _parsedCommitMsg.version
                            _namedTag = _parsedCommitMsg.namedTag
                            echo("This is a build commit.")
                            echo("buildVersion: [${_buildVersion}], namedTag: [${_namedTag}]")

                            env.NAMED_TAG = _namedTag
                            env.BUILD_STABLE_DOCKER_IMAGE = true
                            edgeXSemver('init', _buildVersion)  // <-- Generates a VERSION file and .semver directory
                        }
                        else {
                            echo("This is not a build commit.")
                            edgeXSemver 'init' // <-- Generates a VERSION file and .semver directory
                            env.BUILD_STABLE_DOCKER_IMAGE = false
                        }
                    }
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
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            if(!fileExists('go.sum') && env.GO_VERSION =~ '1.16') {
                                                sh 'go mod tidy' // for Go 1.16
                                            }
                                            sh "${env.TEST_SCRIPT}"
                                        }
                                        sh 'sudo chown -R jenkins:jenkins .' // fix perms
                                        stash name: 'coverage-report', includes: '**/*coverage.out', useDefaultExcludes: false, allowEmpty: true
                                    }
                                }
                            }

                            stage('Build') {
                                when { environment name: 'SHOULD_BUILD', value: 'true' }
                                steps {
                                    buildArtifact()
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
                                        taggedAMD64Images = edgeXDocker.push("${env.DOCKER_IMAGE_NAME}", true, "${env.DOCKER_NEXUS_REPO}")
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
                                        if(params.CommitId) {
                                            sh "git checkout ${params.CommitId}"
                                        }
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
                                // need to always run this stage due to codecov always needing the coverage.out file
                                // when {
                                //     expression { !edgex.isReleaseStream() }
                                // }
                                steps {
                                    script {
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            if(!fileExists('go.sum') && env.GO_VERSION =~ '1.16') {
                                                sh 'go mod tidy' // for Go 1.16
                                            }
                                            sh "${env.TEST_SCRIPT}"
                                        }
                                        sh 'sudo chown -R jenkins:jenkins .' // fix perms
                                        stash name: 'coverage-report', includes: '**/*coverage.out', useDefaultExcludes: false, allowEmpty: true
                                    }
                                }
                            }

                            stage('Build') {
                                when { environment name: 'SHOULD_BUILD', value: 'true' }
                                steps {
                                    script {
                                        buildArtifact()
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
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)  // TODO: this should not be needed anymore
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

            // CodeCov should only run once, no need to run per arch only
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

            // Scan Go Dependencies (snyk monitor)
            stage('Snyk Dependency Scan') {
                when { expression { edgex.isReleaseStream() } }
                steps {
                    edgeXSnyk()
                }
            }

            stage('Archive Prep') {
                when {
                    expression { env.ARTIFACT_TYPES.split(' ').contains('archive') }
                }
                steps {
                    script {
                        // unstash artifacts back on main node, edgeXInfraPublish will archive
                        if(edgex.nodeExists(config, 'amd64')) {
                            unstash 'artifacts-x86_64'
                        }
                        if(edgex.nodeExists(config, 'arm64')) {
                            unstash 'artifacts-arm64'
                        }
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
                            script {
                                unstash 'semver'

                                def tagCommand = 'tag'
                                def _commitMsg = edgex.getCommitMessage(env.GIT_COMMIT)
                                if(edgex.isBuildCommit(_commitMsg)) {
                                    tagCommand = 'tag -force'
                                }

                                edgeXSemver "${tagCommand}"
                                edgeXInfraLFToolsSign(command: 'git-tag', version: 'v${VERSION}')
                            }
                        }
                    }
                    stage('Bump Pre-Release Version') {
                        steps {
                            edgeXSemver "bump ${env.SEMVER_BUMP_LEVEL}"
                            edgeXSemver 'push'
                        }
                    }
                    stage('Bump Experimental Tag') {
                        when {
                            allOf {
                                environment name: 'BUILD_EXPERIMENTAL_DOCKER_IMAGE', value: 'true'
                                expression { env.SEMVER_BRANCH =~ /^main$/ }
                                // env.GITSEMVER_HEAD_TAG is only set when HEAD is tagged and
                                // when set - edgeXSemver will ignore all tag, bump and push commands (unforced)
                                // thus we also want to ignore updating stable/experimental tags when set
                                expression { env.GITSEMVER_HEAD_TAG == null }
                            }
                        }
                        steps {
                            script {
                                edgeXUpdateNamedTag(env.GITSEMVER_INIT_VERSION, 'experimental')
                            }
                        }
                    }
                    stage('Bump Stable (Named) Tag') {
                        when {
                            allOf {
                                environment name: 'BUILD_STABLE_DOCKER_IMAGE', value: 'true'
                                expression { env.SEMVER_BRANCH =~ /^main$/ }
                                // env.GITSEMVER_HEAD_TAG is only set when HEAD is tagged and
                                // when set - edgeXSemver will ignore all tag, bump and push commands (unforced)
                                // thus we also want to ignore updating stable/experimental tags when set
                                expression { env.GITSEMVER_HEAD_TAG == null }
                            }
                        }
                        steps {
                            script {
                                edgeXUpdateNamedTag(env.GITSEMVER_INIT_VERSION, env.NAMED_TAG)
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

def prepBaseBuildImage() {
    def baseImage = env.DOCKER_BASE_IMAGE

    if(env.ARCH == 'arm64' && baseImage.contains(env.DOCKER_REGISTRY)) {
        baseImage = "${env.DOCKER_BASE_IMAGE}".replace('edgex-golang-base', "edgex-golang-base-${env.ARCH}")
    }

    edgex.bannerMessage "[edgeXBuildGoApp] Building Code With image [${baseImage}]"

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

def buildArtifact() {
    def artifactTypes = env.ARTIFACT_TYPES.split(' ') ?: []

    if(artifactTypes.contains('docker')) {
        def imageName = (env.ARCH == 'x86_64') ? env.DOCKER_IMAGE_NAME : "${env.DOCKER_IMAGE_NAME}-${env.ARCH}"
        edgeXDocker.build(imageName, "ci-base-image-${env.ARCH}")
    }

    if(artifactTypes.contains('archive')) {
        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
            sh env.BUILD_SCRIPT
            // change permissions of archive root parent directory
            sh 'chown -R 1001:1001 ${ARTIFACT_ROOT}/..'
        }
        stash name: "artifacts-${env.ARCH}", includes: "${env.ARTIFACT_ROOT}/**", useDefaultExcludes: false, allowEmpty: true
    }
}

def validate(config) {
    if(!config.project) {
        error('[edgeXBuildGoApp] The parameter "project" is required. This is typically the project name.')
    }
}

def toEnvironment(config) {
    def _projectName   = config.project
    def _mavenSettings = config.mavenSettings ?: "${_projectName}-settings"

    if(env.SILO == 'sandbox') {
        _mavenSettings = 'sandbox-settings'
    }

    def _useSemver     = edgex.defaultTrue(config.semver)
    def _testScript    = config.testScript ?: 'make test'
    def _buildScript   = config.buildScript ?: 'make build'
    def _goVersion     = config.goVersion ?: '1.16'
    def _goProxy       = config.goProxy ?: 'https://nexus3.edgexfoundry.org/repository/go-proxy/'
    def _useAlpine     = edgex.defaultTrue(config.useAlpineBase)

    def _dockerBaseImage     = edgex.getGoLangBaseImage(_goVersion, _useAlpine)
    def _dockerFilePath      = config.dockerFilePath ?: 'Dockerfile'
    def _dockerBuildFilePath = config.dockerBuildFilePath ?: 'Dockerfile.build'
    def _dockerBuildContext  = config.dockerBuildContext ?: '.'
    def _dockerBuildImageTarget = config.dockerBuildImageTarget ?: 'builder'
    def _dockerBuildArgs     = config.dockerBuildArgs ?: []
    def _dockerNamespace     = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerImageName     = config.dockerImageName ?: _projectName.replaceAll('-go', '')
    def _dockerNexusRepo     = config.dockerNexusRepo ?: 'staging'
    def _buildImage          = edgex.defaultTrue(config.buildImage)
    def _pushImage           = edgex.defaultTrue(config.pushImage)
    def _semverBump          = config.semverBump ?: 'pre'
    def _semverVersion       = config.semverVersion ?: '' // default to empty string because it is a falsey value
    // def _snapChannel         = config.snapChannel ?: 'latest/edge'
    def _buildSnap           = edgex.defaultFalse(config.buildSnap)
    def _publishSwaggerDocs  = edgex.defaultFalse(config.publishSwaggerDocs)
    def _swaggerApiFolders   = config.swaggerApiFolders ?: ['openapi/v1']
    def _failureNotify       = config.failureNotify ?: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org'

    def _buildExperimentalDockerImage  = edgex.defaultFalse(config.buildExperimentalDockerImage)
    def _buildStableDockerImage        = false

    def _artifactTypes = config.artifactTypes ?: ['docker']

    // if you write files to the archives directory, they are automatically pushed to nexus in the "post" build stage
    def _artifactRoot = config.artifactRoot ?: "archives/bin"

    // needs to be a relative path no starting with ./
    // might try to find a better way to do this in case of absolute path
    if(_artifactRoot.indexOf('./') == 0) {
        _artifactRoot = _artifactRoot.replaceAll('\\.\\/', '')
    }

    def _archiveArtifacts = []

    // new env var to determine if build stage should trigger
    def _shouldBuild = false

    if(!_artifactTypes.contains('docker')) {
        _buildImage = false
    }

    if(_buildImage && _artifactTypes.contains('docker')) {
        _shouldBuild = true
    } else if(_artifactTypes.contains('archive')) {
        _shouldBuild = true

        _archiveArtifacts << '**/*.tar.gz'
        _archiveArtifacts << '**/*.zip'
    }

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
        //GOPROXY: _goProxy,
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
        BUILD_EXPERIMENTAL_DOCKER_IMAGE: _buildExperimentalDockerImage,
        BUILD_STABLE_DOCKER_IMAGE: _buildStableDockerImage,
        SEMVER_BUMP_LEVEL: _semverBump,
        // SNAP_CHANNEL: _snapChannel,
        BUILD_SNAP: _buildSnap,
        PUBLISH_SWAGGER_DOCS: _publishSwaggerDocs,
        SWAGGER_API_FOLDERS: _swaggerApiFolders.join(' '),
        ARTIFACT_ROOT: _artifactRoot,
        ARTIFACT_TYPES: _artifactTypes.join(' '),
        SHOULD_BUILD: _shouldBuild,
        BUILD_FAILURE_NOTIFY_LIST: _failureNotify
    ]

    // encode with comma in case build arg has space
    if(_dockerBuildArgs) {
        envMap << [ DOCKER_BUILD_ARGS: _dockerBuildArgs.join(',')]
    }

    // this is used by global-jjb/shell/logs-deploy.sh to deploy artifacts
    if(_archiveArtifacts) {
        envMap << [ ARCHIVE_ARTIFACTS: _archiveArtifacts.join(' ') ]
    }

    edgex.bannerMessage "[edgeXBuildGoApp] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}