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
releaseStream: 'main'
commitId: '0cc1d67607642c9413e4a80d25a2df35ecc76d41'
repo: 'https://github.com/edgexfoundry/sample-service.git'
gitTag: true
semverBumpLevel: 'patch'  # optional and defaults to '-pre=dev pre'

edgeXReleaseGitTag(releaseYaml)

*/

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
    if(!releaseInfo.commitId) {
        error("[edgeXReleaseGitTag]: Release yaml does not contain 'commitId'")
    }
}

def getSSHRepoName(repo) {
    // return git ssh address for http repo
    repo.replaceAll("https://github.com/", "git@github.com:")
}

def signGitTag(version, name) {
    // call edgeXInfraLFToolsSign to sign git tag version
    println "[edgeXReleaseGitTag]: signing tag: v${version} - DRY_RUN: ${env.DRY_RUN}"
    if(edgex.isDryRun()) {
        echo("edgeXInfraLFToolsSign(command: git-tag version: v${version})")
    }
    else {
        dir("${name}") {
            edgeXInfraLFToolsSign(command: "git-tag", version: "v${version}")
        }
    }
}

def releaseGitTag(releaseInfo, credentials, bump = true, tag = true) {
    // exception handled function that clones, sets and signs git tag version
    try {
        edgeXReleaseGitTag.cloneRepo(releaseInfo.repo, releaseInfo.releaseStream, releaseInfo.name, releaseInfo.commitId, credentials)
        
        def semverBranch = releaseInfo.lts ? releaseInfo.releaseName : releaseInfo.releaseStream

        withEnv(["SEMVER_BRANCH=${semverBranch}"]) {
            if (tag){
                edgeXReleaseGitTag.setAndSignGitTag(releaseInfo.name, releaseInfo.version)
            }
            def semverBumpLevel = releaseInfo.semverBumpLevel ?: '-pre=dev pre'
            edgeXReleaseGitTag.bumpAndPushGitTag(releaseInfo.name, releaseInfo.version, semverBumpLevel, bump)
        }
    }
    catch(Exception ex) {
        error("[edgeXReleaseGitTag]: ERROR occurred releasing git tag: ${ex}")
    }
}
