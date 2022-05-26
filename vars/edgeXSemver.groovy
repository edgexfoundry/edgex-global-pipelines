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
 # edgeXSemver

 ## Overview

 Shared library containing a useful set of functions to help with the creation of semantic versioning using the git-semver python library.
 The main call function builds the `git semver` command based on the provided input.

 **Please note:** this shared library is responsible for setting the `VERSION` environment variable during `git semver init` execution.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 command | false | str | Specify which git semver sub command to run. <br /><br />**Example:** `init`, `bump`, `push` |
 semverVersion | false | string | Force a specific override version instead of reading the version from the semver branch. <br /><br />**Default:** `<empty string>` |
 gitSemverVersion | false | string | What version of the git-semver docker image to use. <br /><br />**Default:** `latest` |
 credentials | false | string | Which Jenkins credential to use to authenticate to GitHub and push git tag. <br /><br />**Default:** `edgex-jenkins-ssh` |

 ## Functions
 - `edgeXSemver.executeGitSemver`: Execute semverCommand via ssh with provided credentials.
 - `edgeXSemver.setGitSemverHeadTag`: set `GITSEMVER_HEAD_TAG` to value of `HEAD` when any of the following conditions are satisfied:
   - An init version is specified and HEAD is tagged with init version.
   - An init version is not specified and HEAD is tagged.
 - `edgeXSemver.getCommitTags`: Return list of all tags at a specific commit point.
 
 ## Usage

 Regular init

 ```groovy
 edgeXSemver('init')
 ```

 Force specific the semver version to use.

 ```groovy
 edgeXSemver('init', '2.0.0')
 ```

 Bump the semver version using default semver bump level (pre-release).

 ```groovy
 edgeXSemver('bump')
 ```
*/

def call(command = null, semverVersion = '', gitSemverVersion = 'latest', credentials = 'edgex-jenkins-ssh') {
    def semverImage = env.ARCH && env.ARCH == 'arm64'
        ? "nexus3.edgexfoundry.org:10003/edgex-devops/py-git-semver-arm64:0.1.4"
        : "nexus3.edgexfoundry.org:10003/edgex-devops/py-git-semver:0.1.4"

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
            semverCommand << "--version=${semverVersion}"
            semverCommand << "--force"
        }

        setupKnownHosts()

        docker.image(semverImage).inside('-u 0:0 -v /etc/ssh:/etc/ssh') {
            withEnv(envVars) {
                if((env.GITSEMVER_HEAD_TAG) && (command != 'init')) {
                    // setting and checking GITSEMVER_HEAD_TAG is the pattern we implement to facilitate re-execution
                    // of jobs without having semver unwantingly tag HEAD with the next version
                    echo "[edgeXSemver]: ignoring command ${command} because GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
                }
                else {
                    if(command == 'init') {
                        // params are treated as environment variables so params.CommitId == env.CommitId
                        setGitSemverHeadTag(semverVersion, credentials, env.CommitId)
                    }
                    executeGitSemver(credentials, semverCommand.join(' '))
                }
            }

            if(!semverVersion) {
                // if no specified version then get next version from git semver
                semverVersion = sh(script: 'git semver', returnStdout: true).trim()
            }
        }

        // Fix semver directory permissions back on the host. docker.image.inside above
        // uses -u 0:0 so .semver directory is owned by root
        if(command =~ '^init') {
            sh 'sudo chown -R jenkins:jenkins .semver'
        }
    }

    env.VERSION = semverVersion

    if(command == 'init') {
        writeFile file: 'VERSION', text: semverVersion
        stash name: 'semver', includes: 'VERSION', useDefaultExcludes: false
        echo "[edgeXSemver]: initialized semver on version ${semverVersion}"
        env.GITSEMVER_INIT_VERSION = semverVersion
    }

    semverVersion
}

// Temp fix for github.com ssh issue and go-git being so strict on known hosts
def setupKnownHosts() {
    sh '''
    if ! grep "github.com ecdsa" /etc/ssh/ssh_known_hosts; then
        grep -v github /etc/ssh/ssh_known_hosts > /tmp/ssh_known_hosts
        if [ -e /tmp/ssh_known_hosts ]; then
            sudo mv /tmp/ssh_known_hosts /etc/ssh/ssh_known_hosts
            echo "github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=" | sudo tee -a /etc/ssh/ssh_known_hosts
        fi
    fi
    '''
}

def executeGitSemver(credentials, semverCommand) {
    sshagent (credentials: [credentials]) {
        if(semverCommand =~ '^.*tag.*-force$') {
            // remove the GITSEMVER_INIT_VERSION tag from remote and local (if it exists)
            // so the tag command will not error with the tag already exists
            // this is an edge condition that only happens when user re-submits a build commit with the same version
            echo "[edgeXSemver]: removing remote and local tags for v${env.GITSEMVER_INIT_VERSION}"
            sh """
                set -x
                set +e
                git ls-remote --tags origin
                git push origin :refs/tags/v${env.GITSEMVER_INIT_VERSION}
                git tag -d v${env.GITSEMVER_INIT_VERSION}
                set -e
            """
        }
        sh semverCommand
    }
}

def setGitSemverHeadTag(initVersion, credentials, pointsAt = null) {
    if(!pointsAt) { pointsAt = 'HEAD' }
    if(env.GITSEMVER_HEAD_TAG) {
        echo "[edgeXSemver]: GITSEMVER_HEAD_TAG is already set to '${env.GITSEMVER_HEAD_TAG}'"
    }
    else {
        def headTags = getCommitTags(credentials, pointsAt)
        if(initVersion) {
            if(headTags.contains("v${initVersion}")) {
                echo "[edgeXSemver]: ${pointsAt} is already tagged with v${initVersion}"
                def gitSemverHeadTags = headTags.join('|')
                env.GITSEMVER_HEAD_TAG = gitSemverHeadTags
                echo "[edgeXSemver]: set GITSEMVER_HEAD_TAG to \'${gitSemverHeadTags}\'"
            }
        }
        else {
            if(headTags) {
                def gitSemverHeadTags = headTags.join('|')
                env.GITSEMVER_HEAD_TAG = gitSemverHeadTags
                echo "[edgeXSemver]: set GITSEMVER_HEAD_TAG to \'${gitSemverHeadTags}\'"
            }
        }
    }
}

def getCommitTags(credentials, pointsAt) {
    def commitTags = []
    def tags
    sshagent (credentials: [credentials]) {
        tags = sh(script: "git tag --points-at ${pointsAt}", returnStdout: true).trim()
    }
    if(tags) {
        commitTags = tags.split()
    }
    commitTags
}