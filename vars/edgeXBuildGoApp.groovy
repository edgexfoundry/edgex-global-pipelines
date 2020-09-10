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
                        env.OG_VERSION = env.VERSION
                        echo("Archived original version: [${env.OG_VERSION}]")
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
                                        sh 'go version'
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
                                        sh 'go version'
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
                    stage('Bump Experimental Tag') {
                        when {
                            allOf {
                                environment name: 'BUILD_EXPERIMENTAL_DOCKER_IMAGE', value: 'true'
                                expression { env.SEMVER_BRANCH =~ /^master$/ }
                            }
                        }
                        steps {
                            script {
                                edgeXUpdateNamedTag(env.OG_VERSION, 'experimental')
                            }
                        }
                    }
                    stage('Bump Stable (Named) Tag') {
                        when {
                            allOf {
                                environment name: 'BUILD_STABLE_DOCKER_IMAGE', value: 'true'
                                expression { env.SEMVER_BRANCH =~ /^master$/ }
                            }
                        }
                        steps {
                            script {
                                edgeXUpdateNamedTag(env.OG_VERSION, env.NAMED_TAG)
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
        SWAGGER_API_FOLDERS: _swaggerApiFolders.join(' ')
    ]

    // encode with comma in case build arg has space
    if(_dockerBuildArgs) {
        envMap << [ DOCKER_BUILD_ARGS: _dockerBuildArgs.join(',')]
    }

    edgex.bannerMessage "[edgeXBuildGoApp] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}
