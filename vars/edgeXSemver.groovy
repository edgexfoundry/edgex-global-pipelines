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

def call(command = null, semverVersion = '', gitSemverVersion = 'latest', credentials = 'edgex-jenkins-ssh', debug = true) {
    def semverImage = env.ARCH && env.ARCH == 'arm64'
        ? "nexus3.edgexfoundry.org:10004/edgex-devops/git-semver-arm64:${gitSemverVersion}"
        : "nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:${gitSemverVersion}"

    def envVars = [
        'SSH_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts'
    ]
    def semverCommand = [
       'git',
       'semver'
    ]

    if(!command) {
        docker.image(semverImage).inside {
            semverVersion = sh(script: 'git semver', returnStdout: true).trim()
        }
    }
    else {
        if(debug) {
            envVars << 'SEMVER_DEBUG=on'
        }
        semverCommand << command

        // If semverVersion is passed in override the version from the .semver directory
        if (command == 'init' && semverVersion) {
            semverCommand << "-ver=${semverVersion}"
            semverCommand << "-force"

        }

        docker.image(semverImage).inside('-v /etc/ssh:/etc/ssh') {
            withEnv(envVars) {
                if((env.GITSEMVER_HEAD_TAG) && (command != 'init')) {
                    println "Ignoring command ${command} because GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
                }
                else {
                    if(command == 'init') {
                        setHeadTagEnv(credentials)
                    }
                    executeSSH(credentials, semverCommand.join(' '))
                }
            }

            // If no version is passed in then get the next version from git semver
            if(!semverVersion) {
                semverVersion = sh(script: 'git semver', returnStdout: true).trim()
            }
        }
    }

    env.setProperty('VERSION', semverVersion)

    if(command == 'init') {
        writeFile file: 'VERSION', text: semverVersion
        stash name: 'semver', includes: '.semver/**,VERSION', useDefaultExcludes: false
        echo "[edgeXSemver] initialized semver on version ${semverVersion}"
    }

    semverVersion
}

def executeSSH(credentials, command) {
    // execute command via ssh with provided credentials considering noop mode
    sshagent (credentials: [credentials]) {
        sh command
    }
}

def setHeadTagEnv(credentials) {
    // set environment variable GITSEMVER_HEAD_TAG to value of tag at HEAD
    if(env.GITSEMVER_HEAD_TAG) {
        println "envvar GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
        return
    }
    try {
        sshagent (credentials: [credentials]) {
            def tag = sh(script: 'git describe --exact-match --tags HEAD', returnStdout: true).trim()
            println "Setting envvar GITSEMVER_HEAD_TAG value to \'${tag}\'"
            env.setProperty('GITSEMVER_HEAD_TAG', tag)
        }
    }
    catch(error) {
        println "[WARNING]: exception occurred checking if HEAD is tagged: ${error}\nThis usually means this commit has not been tagged."
    }
}

def isHeadTagEnv(credentials='edgex-jenkins-ssh') {
    // return true if HEAD is tagged false otherwise
    setHeadTagEnv(credentials)
    return env.GITSEMVER_HEAD_TAG ? true : false
}