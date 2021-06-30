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
releaseStream: 'main'
repo: 'https://github.com/edgexfoundry/sample-service.git'
gitHubReleaseAssets:
  - 'https://nexus-location/asset1'
  - 'https://nexus-location/asset2'
  - 'https://nexus-location/asset3'
gitHubRelease: true

edgeXReleaseGitHubAssets(releaseYaml)

*/

def call(releaseInfo) {
    validate(releaseInfo)
    createGitHubRelease(releaseInfo)
}

def validate(releaseInfo) {
    // raise error if releaseInfo map does not contain required attributes
    if(!releaseInfo.repo) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'repo'")
    }

    if(!releaseInfo.version) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'version'")
    }

    if(!releaseInfo.gitHubReleaseAssets) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'gitHubReleaseAssets'")
    }
}

def getCredentialsId() {
    // return correct credentialId for environment
    (env.SILO == 'production') ? 'edgex-jenkins-github-personal-access-token' : 'edgex-jenkins-access-username'
}

def getRepoInfo(repo) {
    // extracts pertinent information from repo and returns as map
    try {
        def repoSplit = repo.split('/')
        def repoMap = [
            ORG: repoSplit[3],
            REPO: repoSplit[4].replaceAll('.git', '')
        ]
        repoMap
    }
    catch(Exception ex) {
        error("[edgeXReleaseGitHubAssets]: Release yaml 'repo' value is malformed")
    }
}

def createGitHubRelease(releaseInfo) {
    repoInfo = getRepoInfo(releaseInfo.repo)
    def githubReleaseImage = 'nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest'
    def command = [
        'create-github-release',
        "--repo '${repoInfo.ORG}/${repoInfo.REPO}'",
        "--tag 'v${releaseInfo.version}'",
        "--assets 'assets/'"
    ]
    docker.image(githubReleaseImage).inside {
        sh 'mkdir assets'
        releaseInfo.gitHubReleaseAssets.each { asset ->
            sh "wget ${asset} -P assets/"
        }
        println "[edgeXReleaseGitHubAssets] command to run: ${command.join(' ')}"
        if(!edgex.isDryRun()) {
            def credentialsId = getCredentialsId()
            withCredentials([
                usernamePassword(
                    credentialsId: credentialsId,
                    usernameVariable: 'GH_TOKEN_USR',
                    passwordVariable: 'GH_TOKEN_PSW')]) {
                sh command.join(' ')
            }
        }
    }
}