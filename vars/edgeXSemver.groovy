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

def call(command = null, semverVersion = '', gitSemverVersion = 'latest', credentials = 'edgex-jenkins-ssh') {
    def semverImage = env.ARCH && env.ARCH == 'arm64'
        ? "nexus3.edgexfoundry.org:10004/edgex-devops/git-semver-arm64:${gitSemverVersion}"
        : "nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:${gitSemverVersion}"

    def envVars = [
        'SSH_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts',
        'SEMVER_DEBUG=on'
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
        semverCommand << command

        // If semverVersion is passed in override the version from the .semver directory
        if (command == 'init' && semverVersion) {
            semverCommand << "-ver=${semverVersion}"
            semverCommand << "-force"
        }

        docker.image(semverImage).inside('-v /etc/ssh:/etc/ssh') {
            withEnv(envVars) {
                if((env.GITSEMVER_HEAD_TAG) && (command != 'init')) {
                    // setting and checking GITSEMVER_HEAD_TAG is the pattern we implement to facilitate re-execution
                    // of jobs without having semver unwantingly tag HEAD with the next version
                    echo "[edgeXSemver]: ignoring command ${command} because GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
                }
                else {
                    if((command == 'init') && (!semverVersion)) {
                        // when a semverVersion is specified semver will force init the version and
                        // we don't want to set GITSEMVER_HEAD_TAG since we don't want to ignore
                        // any other subsequent git semver commands associated with the force init
                        setGitSemverHeadTag(credentials)
                    }
                    executeGitSemver(credentials, semverCommand.join(' '))
                }
            }

            if(!semverVersion) {
                // if no specified version then get next version from git semver
                semverVersion = sh(script: 'git semver', returnStdout: true).trim()
            }
        }
    }

    env.VERSION = semverVersion

    if(command == 'init') {
        writeFile file: 'VERSION', text: semverVersion
        stash name: 'semver', includes: '.semver/**,VERSION', useDefaultExcludes: false
        echo "[edgeXSemver]: initialized semver on version ${semverVersion}"

        if(semverVersion) {
            // set GITSEMVER_INIT_VERSION to initial specified version
            env.GITSEMVER_INIT_VERSION = semverVersion
        }
    }

    semverVersion
}

def executeGitSemver(credentials, semverCommand) {
    // execute semverCommand via ssh with provided credentials
    sshagent (credentials: [credentials]) {
        if(semverCommand =~ '^.*tag.*$') {
            // edgeXSemver sets specified semverVersion in the GITSEMVER_INIT_VERSION environment variable
            def headTags = getHeadTags(credentials)
            if(headTags.contains("v${env.GITSEMVER_INIT_VERSION}")) {
                // if HEAD is already tagged with GITSEMVER_INIT_VERSION then return and do not execute the tag command
                echo "[edgeXSemver]: HEAD is already tagged with ${env.GITSEMVER_INIT_VERSION}"
                return
            }
            else {
                if(semverCommand =~ '^.*-force$') {
                    // HEAD is not tagged with GITSEMVER_INIT_VERSION
                    // lets remove the GITSEMVER_INIT_VERSION tag from remote and local (if it exists)
                    // so the tag command will not error with the tag already exists
                    // this is an edge condition that only happens when user re-submits a build commit on same version
                    echo "[edgeXSemver]: removing remote and local tags for ${env.GITSEMVER_INIT_VERSION}"
                    sh """
                        set +x
                        set +e
                        git push origin :refs/tags/${env.GITSEMVER_INIT_VERSION}
                        git tag -d ${env.GITSEMVER_INIT_VERSION}
                        set -e
                    """
                }
            }
        }
        sh semverCommand
    }
}

def setGitSemverHeadTag(credentials) {
    // set GITSEMVER_HEAD_TAG to value of all tags at HEAD only if tagged
    if(env.GITSEMVER_HEAD_TAG) {
        echo "[edgeXSemver]: GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
    }
    else {
        def headTags = getHeadTags(credentials)
        if(headTags) {
            def gitSemverHeadTags = headTags.join('|')
            env.GITSEMVER_HEAD_TAG = gitSemverHeadTags
            echo "[edgeXSemver]: set GITSEMVER_HEAD_TAG to \'${gitSemverHeadTags}\'"
        }
    }
}

def getHeadTags(credentials) {
    // return list of all tags at HEAD
    def headTags = []
    def tags
    sshagent (credentials: [credentials]) {
        tags = sh(script: 'git tag --points-at HEAD', returnStdout: true).trim()
    }
    if(tags) {
        headTags = tags.split()
    }
    headTags
}