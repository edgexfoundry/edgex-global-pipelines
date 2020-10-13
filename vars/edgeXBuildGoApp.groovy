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

 #edgeXBuildGoApp

 Shared Library to build Go projects

 ##Parameters
 * **project** - Specify your project name
 * **goVersion** - Go version

 ##Example Usage

 ```bash
 edgeXBuildGoApp (
 project: 'device-random-go',
 goVersion: '1.13'
 )
 ```
 ```bash
 edgeXBuildGoApp (
 project: 'app-functions-sdk-go',
 semver: true,
 goVersion: '1.13',
 testScript: 'make test',
 buildImage: false,
 publishSwaggerDocs: true,
 swaggerApiFolders: ['openapi/v2']
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
                                        // should this be in it's own stage?
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
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            sh "${TEST_SCRIPT}"
                                            stash name: 'coverage-report', includes: '**/*coverage.out', useDefaultExcludes: false, allowEmpty: true
                                        }
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
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        taggedAMD64Images = edgeXDocker.push("${env.DOCKER_IMAGE_NAME}", true, "${env.DOCKER_NEXUS_REPO}")
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
                                        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
                                            sh "${env.TEST_SCRIPT}"
                                            stash name: 'coverage-report', includes: '**/*coverage.out', useDefaultExcludes: false, allowEmpty: true
                                        }
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
                                        edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                        taggedARM64Images = edgeXDocker.push("${env.DOCKER_IMAGE_NAME}-${env.ARCH}", true, "${env.DOCKER_NEXUS_REPO}")
                                    }
                                }
                            }

                            stage('Snap') {
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
                                    edgeXSnap(jobType: 'build')
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
                when {
                    allOf {
                        environment name: 'SILO', value: 'production'
                        expression { !edgex.isReleaseStream() }
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
                        if(edgex.nodeExists(config, 'amd64') && taggedAMD64Images) {
                            edgeXClair(taggedAMD64Images.first())
                        }
                        if(edgex.nodeExists(config, 'arm64') && taggedARM64Images) {
                            edgeXClair(taggedARM64Images.first())
                        }
                    }
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
                                expression { env.SEMVER_BRANCH =~ /^master$/ }
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
                                expression { env.SEMVER_BRANCH =~ /^master$/ }
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
        baseImage = "${env.DOCKER_BASE_IMAGE}".replace('edgex-golang-base', "edgex-golang-base-${env.ARCH}")
    }

    edgex.bannerMessage "[edgeXBuildGoApp] Building Code With image [${baseImage}]"

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

def buildArtifact() {
    def artifactTypes = env.ARTIFACT_TYPES.split(' ') ?: []

    if(artifactTypes.contains('docker')) {
        def imageName = (env.ARCH == 'x86_64') ? env.DOCKER_IMAGE_NAME : "${env.DOCKER_IMAGE_NAME}-${env.ARCH}"
        edgeXDocker.build(imageName, "ci-base-image-${env.ARCH}")
    }

    if(artifactTypes.contains('archive')) {
        docker.image("ci-base-image-${env.ARCH}").inside('-u 0:0') {
            sh env.BUILD_SCRIPT
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
    def _goVersion     = config.goVersion ?: '1.15'
    def _goProxy       = config.goProxy ?: 'https://nexus3.edgexfoundry.org/repository/go-proxy/'
    def _useAlpine     = edgex.defaultTrue(config.useAlpineBase)

    def _dockerBaseImage     = edgex.getGoLangBaseImage(_goVersion, _useAlpine)
    def _dockerFilePath      = config.dockerFilePath ?: 'Dockerfile'
    def _dockerBuildFilePath = config.dockerBuildFilePath ?: 'Dockerfile.build'
    def _dockerBuildContext  = config.dockerBuildContext ?: '.'
    def _dockerBuildArgs     = config.dockerBuildArgs ?: []
    def _dockerNamespace     = config.dockerNamespace ?: '' //default for edgex is empty string
    def _dockerImageName     = config.dockerImageName ?: "docker-${_projectName}"
    def _dockerNexusRepo     = config.dockerNexusRepo ?: 'staging'
    def _buildImage          = edgex.defaultTrue(config.buildImage)
    def _pushImage           = edgex.defaultTrue(config.pushImage)
    def _semverBump          = config.semverBump ?: 'pre'
    def _semverVersion       = config.semverVersion ?: '' // default to empty string because it is a falsey value
    // def _snapChannel         = config.snapChannel ?: 'latest/edge'
    def _buildSnap           = edgex.defaultFalse(config.buildSnap)
    def _publishSwaggerDocs  = edgex.defaultFalse(config.publishSwaggerDocs)
    def _swaggerApiFolders   = config.swaggerApiFolders ?: ['openapi/v1']

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
        GOPROXY: _goProxy,
        DOCKER_BASE_IMAGE: _dockerBaseImage,
        DOCKER_FILE_PATH: _dockerFilePath,
        DOCKER_BUILD_FILE_PATH: _dockerBuildFilePath,
        DOCKER_BUILD_CONTEXT: _dockerBuildContext,
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
        SHOULD_BUILD: _shouldBuild
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
