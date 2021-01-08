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
commitId: '0cc1d67607642c9413e4a80d25a2df35ecc76d41'
repo: 'https://github.com/edgexfoundry/sample-service.git'
gitTag: true
semverBumpLevel: 'patch'  # optional and defaults to '-pre=dev pre'

edgeXReleaseGitTag(releaseYaml)

*/

def call(releaseInfo, releaseGitTagOptions) {
    def credentials = releaseGitTagOptions.credentials != null ? releaseGitTagOptions.credentials : 'edgex-jenkins-ssh'
    def bump = releaseGitTagOptions.bump != null ? releaseGitTagOptions.bump : 'true'
    def tag = releaseGitTagOptions.tag != null ? releaseGitTagOptions.tag : 'true'

    edgeXReleaseGitTagUtil.validate(releaseInfo)
    edgeXReleaseGitTagUtil.releaseGitTag(releaseInfo, credentials, bump, tag)
}

def cloneRepo(repo, branch, name, commitId, credentials) {
    // clone the repo branch to name using the specified ssh credentials
    def ssh_repo = edgeXReleaseGitTagUtil.getSSHRepoName(repo)
    println "[edgeXReleaseGitTag]: git cloning ${ssh_repo} : ${branch} to ${name} - DRY_RUN: ${env.DRY_RUN}"

    sshagent(credentials: [credentials]) {
        if(edgex.isDryRun()) {
            echo("git clone -b ${branch} ${ssh_repo} ${env.WORKSPACE}/${name} || true")
            echo("dir ${name}")
            echo("git checkout ${commitId}")
        }
        else {
            sh "git clone -b ${branch} ${ssh_repo} ${env.WORKSPACE}/${name} || true"
            dir("${name}") {
                sh "git checkout ${commitId}"
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

def bumpAndPushGitTag(name, version, bumpLevel, bump = true) {
    // call edgeXSemver bump to bump semver branch to next bumpLevel and push to push git tag
    println "[edgeXReleaseGitTag]: pushing git tag for ${name}: ${version} - DRY_RUN: ${env.DRY_RUN}"
    
    def commands = [
        "push"
    ]
    
    if(bump) {
        commands.add(0, "bump ${bumpLevel}")
    }
    
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
