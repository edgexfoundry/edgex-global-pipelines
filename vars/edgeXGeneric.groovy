/*edgeXGeneric([
    project: 'edgex-go',
    arch: ['amd64', 'arm64'],
    semver: true,
    pre_build_script: 'shell/install_custom_golang.sh',
    build_script: 'make test && make build docker',
    post_build_script: 'ls -al',
    env: [ GO_ROOT: '/opt/go-custom/go' ]
])*/

def call(config) {
    config = [
        project: 'edgex-go',
        arch: ['amd64', 'arm64'],
        semver: false,
        mavenSettings: ['edgex-go-settings:SETTINGS_FILE', 'edgex-go-codecov-token:CODECOV_TOKEN'],
        env: [
            GOROOT: '/opt/go-custom/go'
        ],
        path: [
            '/opt/go-custom/go/bin',
            '/some/other/path'
        ],
        branches: [
            '*': [
                pre_build: ['shell/install_custom_golang.sh'],
                build: [
                    'make test raml_verify && make build docker',
                    'shell/codecov-uploader.sh'
                ]
            ],
            'master': [
                post_build: [ 'shell/edgexfoundry-go-docker-push.sh' ]
            ]
        ] 
    ]

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
        }

        stages {
            stage('Prepare') {
                steps {
                    edgeXSetupEnvironment(_envVarMap)

                    dir('.ci-management') {
                        git url: 'https://github.com/edgexfoundry/ci-management.git'
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
                        when { expression { edgex.nodeExists(config, 'amd64') } }
                        agent { label edgex.getNode(config, 'amd64') } // agent gets evaluated before when
                        environment {
                            ARCH = 'x86_64'
                        }
                        stages {
                            stage('Prep VM') {
                                steps {
                                    edgexDockerLogin(env.MAVEN_SETTINGS)
                                    unstash 'ci-management'
                                }
                            }
                            stage('Pre Build') {
                                steps {
                                    script {
                                        sh 'env | sort'
                                        echo '========================================='
                                        withEnv(["PATH=${setupPath(config)}"]) {
                                            sh 'echo $ARCH prebuild'
                                            sh 'env | sort'
                                        }
                                    }
                                }
                            }
                            stage('Build') {
                                steps {
                                    sh 'echo $ARCH build'
                                }
                            }
                            stage('Post Build') {
                                steps {
                                    sh 'echo $ARCH post build'
                                }
                            }
                        }
                    }
                    stage('arm64') {
                        when { expression { edgex.nodeExists(config, 'arm64') } }
                        agent { label edgex.getNode(config, 'arm64') }
                        environment {
                            ARCH = 'arm64'
                        }
                        stages {
                            stage('Prep VM') {
                                steps {
                                    edgexDockerLogin(env.MAVEN_SETTINGS)
                                    unstash 'ci-management'
                                }
                            }
                            stage('Pre Build') {
                                steps {
                                    script {
                                        sh 'env | sort'
                                        echo '========================================='
                                        withEnv(["PATH=${setupPath(config)}"]) {
                                            sh 'echo $ARCH prebuild'
                                            sh 'env | sort'
                                        }
                                    }
                                }
                            }
                            stage('Build') {
                                steps {
                                    sh 'echo $ARCH build'
                                }
                            }
                            stage('Post Build') {
                                steps {
                                    sh 'echo $ARCH post build'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // datadog {
      // Add tags from file in workspace
      // Add tags from list of properties	
      // Discard old builds 30 days
    // }

    // timestamps()
    // timeout(360) { //fail
    // sshagent(edgex-jenkins) //not needed
    // mkdir -p $WORKSPACE/target/classes $WORKSPACE/jacoco/classes
    // global-settings GLOBAL_SETTINGS_FILE ??
    // edgex-go-settings SETTINGS_FILE // config.settingsFile 

    // environment {
    //   GOROOT=/opt/go-custom/go
    //   PATH=$PATH:$GOROOT/bin
    //   GOPATH=$HOME/$BUILD_ID/gopath
    // }

    // install_custom_golang.sh

    // build_script config.buildScript
    // make test raml_verify && make build docker

    // codecov
    // edgeXCodeCov(APP-token)
    
    ////// Post Build ... already done
    // sysstat.sh
    // package-listing.sh
    // edgex-infra-ship-logs.sh
    // build description: ^Build logs: .*
    
    // delete workspace when done .. nope
    // email notification: EdgeX-Jenkins-Alerts+int+444+7674852109629482390@lists.edgexfoundry.org Jenkins Mailer PLugin
    // Send e-mail for every unstable build
}

def validate(config) {
    if(!config.project) {
        error('[edgeXGeneric] The parameter "project" is required. This is typically the project name.')
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

def setupPath(config) {
    println "[DEBUG] edgeXGeneric.setupPath() ${PATH}:${config.path.join(':')}"
    "${PATH}:${config.path.join(':')"
}