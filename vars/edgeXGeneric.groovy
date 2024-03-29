import com.cloudbees.groovy.cps.NonCPS
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
 # edgeXGeneric

 ⚠️ Deprecated will be removed in a future version. DO NOT USE ⚠️

 ```groovy
 edgeXGeneric([
     project: 'edgex-go',
     mavenSettings: ['edgex-go-codecov-token:CODECOV_TOKEN'], (optional)
     credentials: [string(credentialsId: 'credential-id-here', variable: 'APIKEY')], (optional)
     env: [
         GOPATH: '/opt/go-custom/go'
     ],
     path: [
         '/opt/go-custom/go/bin'
     ],
     branches: [
         '*': [
             pre_build: ['shell/install_custom_golang.sh'],
             build: [
                 'make test raml_verify && make build docker',
                 'shell/codecov-uploader.sh'
             ]
         ],
         'main': [
             post_build: [ 'shell/edgexfoundry-go-docker-push.sh' ]
         ]
     ]
 ])
 ```
*/

def cfgAmd64
def cfgArm64

def call(config) {
    edgex.bannerMessage "[edgeXGeneric] RAW Config: ${config}"

    validate(config)
    edgex.setupNodes(config)

    def _envVarMap = toEnvironment(config)

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

                    dir('.ci-management') {
                        git url: 'https://github.com/edgexfoundry/ci-management.git'
                        sh 'chmod +x shell/*'
                    }

                    stash name: 'ci-management', includes: '.ci-management/**', useDefaultExcludes: false
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
                        // agent { label edgex.getNode(config, 'amd64') }
                        environment {
                            ARCH = 'x86_64'
                            GOARCH = 'amd64'
                        }
                        stages {
                            stage('Prep VM') {
                                steps {
                                    edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                    // unstash 'ci-management' no need to unstash this since we are already on the mainNode
                                    script {
                                        cfgAmd64 = getConfigFilesFromEnv()
                                    }
                                }
                            }
                            stage('Pre Build') {
                                when { expression { anyScript(config, 'pre_build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        println "[DEBUG] creds ==============> ${config.credentials}"

                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgAmd64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'pre_build', env.GIT_BRANCH)
                                                    println "$ARCH pre_build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stage('Build') {
                                when { expression { anyScript(config, 'build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgAmd64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'build', env.GIT_BRANCH)
                                                    println "$ARCH build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stage('Post Build') {
                                when { expression { anyScript(config, 'post_build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgAmd64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'post_build', env.GIT_BRANCH)
                                                    println "$ARCH post_build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
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
                            GOARCH = 'arm64'
                        }
                        stages {
                            stage('Prep VM') {
                                steps {
                                    edgeXDockerLogin(settingsFile: env.MAVEN_SETTINGS)
                                    unstash 'ci-management'
                                    script {
                                        cfgArm64 = getConfigFilesFromEnv()
                                    }
                                }
                            }
                            stage('Pre Build') {
                                when { expression { anyScript(config, 'pre_build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgArm64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'pre_build', env.GIT_BRANCH)
                                                    println "$ARCH pre_build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stage('Build') {
                                when { expression { anyScript(config, 'build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgArm64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'build', env.GIT_BRANCH)
                                                    println "$ARCH build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stage('Post Build') {
                                when { expression { anyScript(config, 'post_build', env.GIT_BRANCH) } }
                                steps {
                                    script {
                                        withCredentials(config.credentials) {
                                            configFileProvider(cfgArm64) {
                                                withEnv(["PATH=${setupPath(config)}"]) {
                                                    def scripts = allScripts(config, 'post_build', env.GIT_BRANCH)
                                                    println "$ARCH post_build: ${scripts}"
                                                    scripts.each { userScript ->
                                                        if(userScript.indexOf('shell/') == 0) {
                                                            sh "./.ci-management/${userScript}"
                                                        } else {
                                                            sh userScript
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        post {
                            always {
                                lfParallelCostCapture()
                            }
                        }
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
                            edgeXSemver 'bump pre'
                            edgeXSemver 'push'
                        }
                    }
                }
            }
        }
    }

    // email notification: EdgeX-Jenkins-Alerts+int+444+7674852109629482390@lists.edgexfoundry.org Jenkins Mailer PLugin
    // Send e-mail for every unstable build
}

def validate(config) {
    if(!config.project) {
        error('[edgeXGeneric] The parameter "project" is required. This is typically the project name.')
    }

    // set default credentials if empty or null
    if(!config.credentials) {
        config.credentials = []
    }
}

def toEnvironment(config) {
    def _projectName   = config.project
    def _projectSettings = "${_projectName}-settings:SETTINGS_FILE"

    def _defaultSettings = config.mavenSettings ?: [ _projectSettings ]

    // rebuild maven settings array
    def _mavenSettings
    def _extraSettings = []

    _defaultSettings.each { setting ->
        def settingName = setting.split(':')[0]
        def settingEnvVar = setting.split(':')[1]

        if(setting == _projectSettings) {
            if(env.SILO == 'sandbox') {
                _mavenSettings = 'sandbox-settings'
            } else {
                _mavenSettings = settingName
            }
        }
        else {
            _extraSettings << setting
        }
    }

    def _useSemver = edgex.defaultFalse(config.semver)

    def envMap = [
        MAVEN_SETTINGS: _mavenSettings,
        EXTRA_SETTINGS: _extraSettings.join(','),
        PROJECT: _projectName,
        USE_SEMVER: _useSemver
    ]

    if(config.env) {
        envMap << config.env
    }

    edgex.bannerMessage "[edgeXGeneric] Pipeline Parameters:"
    edgex.printMap envMap

    envMap
}

@NonCPS
def getScripts(config, scriptType, branch) {
    def scripts = []
    def cleanBranch = branch.replaceAll(/origin|upstream/, '').replaceAll('/', '')
    def branches = config.branches.findAll { k,v ->  (k == cleanBranch) }

    branches.each { b,v ->
        if(v && v[scriptType]) {
            scripts.addAll(v[scriptType])
        }
    }

    scripts
}

@NonCPS
def anyScript(config, scriptType, branch) {
    allScripts(config, scriptType, branch) ? true : false
}

@NonCPS
def allScripts(config, scriptType, branch) {
    def s = getScripts(config, scriptType, '*') ?: []

    if(branch) {
        s.addAll(getScripts(config, scriptType, branch))
    }

    s
}

@NonCPS
def getConfigFilesFromEnv() {
    def configFiles = []
    if(env.EXTRA_SETTINGS) {
        configFiles = env.EXTRA_SETTINGS.split(',').collect { file ->
            configFile(fileId: file.split(':')[0], variable: file.split(':')[1])
        }
    }

    configFiles
}

@NonCPS
def setupPath(config) {
    config.path ? "${env.PATH}:${config.path.join(':')}" : "${env.PATH}"
}
