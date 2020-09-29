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

/*

releaseYaml:

---
name: 'sample-service'
version: '1.1.2'
releaseStream: 'master'
repo: 'https://github.com/edgexfoundry/sample-service.git'
gitTag: true
semverBumpLevel: 'patch'  # optional and defaults to '-pre=dev pre'

edgeXReleaseGitTag(releaseYaml)

*/

def call(releaseInfo, credentials = 'edgex-jenkins-ssh') {
    edgeXReleaseGitTagUtil.validate(releaseInfo)
    edgeXReleaseGitTagUtil.releaseGitTag(releaseInfo, credentials)
}

def cloneRepo(repo, branch, name, credentials) {
    // clone the repo branch to name using the specified ssh credentials
    def ssh_repo = edgeXReleaseGitTagUtil.getSSHRepoName(repo)
    println "[edgeXReleaseGitTag]: git cloning ${ssh_repo} : ${branch} to ${name} - DRY_RUN: ${env.DRY_RUN}"
    def commands = [
        "git clone -b ${branch} ${ssh_repo} ${env.WORKSPACE}/${name}",
    ]
    sshagent(credentials: [credentials]) {
        if(edgex.isDryRun()) {
            echo(commands.collect {"sh ${it}"}.join('\n'))
        }
        else {
            commands.each { command ->  // named variable required due to LOL
                sh command
            }
        }
    }
}

def setAndSignGitTag(name, version) {
    // call edgeXSemver functions to force tag version and push
    println "[edgeXReleaseGitTag]: setting tag for ${name} to: ${version} - DRY_RUN: ${env.DRY_RUN}"
    if(edgex.isDryRun()) {
        echo("edgeXSemver init ${version}")
        echo("edgeXSemver tag -force")
    }
    else {
        dir("${name}") {
            edgeXSemver('init', version)
            edgeXSemver('tag -force')
        }
    }
    edgeXReleaseGitTagUtil.signGitTag(version, name)
}

def bumpAndPushGitTag(name, version, bumpLevel) {
    // call edgeXSemver bump to bump semver branch to next bumpLevel and push to push git tag
    println "[edgeXReleaseGitTag]: pushing git tag for ${name}: ${version} - DRY_RUN: ${env.DRY_RUN}"
    def commands = [
        "bump ${bumpLevel}",
        "push"
    ]
    if(edgex.isDryRun()) {
        echo(commands.collect {"edgeXSemver ${it}"}.join('\n'))
    }
    else {
        dir("${name}") {
            commands.each { command ->
                edgeXSemver command
            }
        }
    }
}
