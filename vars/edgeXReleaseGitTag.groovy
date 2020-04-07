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
version: 1.1.2
releaseStream: master
repo: 'https://github.com/edgexfoundry/sample-service.git'

edgeXReleaseGitTag(releaseYaml)

*/

def call(releaseInfo, credentials = 'edgex-jenkins-ssh') {
    validate(releaseInfo)
    releaseGitTag(releaseInfo, credentials)
}

def validate(releaseInfo) {
    // raise error if releaseInfo map does not contain required attributes
    if(!releaseInfo.name) {
        error("[edgeXReleaseGitTag]: Release yaml does not contain 'name'")
    }
    if(!releaseInfo.version) {
        error("[edgeXReleaseGitTag]: Release yaml does not contain 'version'")
    }
    if(!releaseInfo.releaseStream) {
        error("[edgeXReleaseGitTag]: Release yaml does not contain 'releaseStream'")
    }
    if(!releaseInfo.repo) {
        error("[edgeXReleaseGitTag]: Release yaml does not contain 'repo'")
    }
}

def getSSHRepoName(repo) {
    // return git ssh address for http repo
    repo.replaceAll("https://github.com/", "git@github.com:")
}

def cloneRepo(repo, branch, name, credentials) {
    // clone the repo branch to name using the specified ssh credentials
    def ssh_repo = getSSHRepoName(repo)
    println "[edgeXReleaseGitTag]: git cloning ${ssh_repo} : ${branch} to ${name}"
    sshagent(credentials: [credentials]) {
        sh "git clone -b ${branch} ${ssh_repo} ${name}"
        sh "cd ${name}"
    }
}

def setGitTag(name, version) {
    // call edgeXSemver functions to force tag version and push
    println "[edgeXReleaseGitTag]: setting tag for ${name} to: ${version}"
    edgeXSemver "init -ver=${version} -force"
    edgeXSemver "tag -force"
    edgeXSemver "push"
}

def signGitTag(version) {
    // call edgeXInfraLFToolsSign to sign git tag version
    println "[edgeXReleaseGitTag]: signing tag: v${version}"
    edgeXInfraLFToolsSign(command: "git-tag", version: "v${version}")
}

def releaseGitTag(releaseInfo, credentials) {
    // exception handled function that clones, sets and signs git tag version
    try {
        cloneRepo(releaseInfo.repo, releaseInfo.releaseStream, releaseInfo.name, credentials)
        setGitTag(releaseInfo.name, releaseInfo.version)
        signGitTag(releaseInfo.version)
    }
    catch(error) {
        echo("[edgeXReleaseGitTag]: ERROR occurred releasing git tag: ${error}")
    }
}
