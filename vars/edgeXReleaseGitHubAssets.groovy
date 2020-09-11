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
releaseStream: 'master'
repo: 'https://github.com/edgexfoundry/sample-service.git'
assets:
  - 'https://nexus-location/asset1'
  - 'https://nexus-location/asset2'
  - 'https://nexus-location/asset3'
gitHubRelease: true

edgeXReleaseGitHubAsset(releaseYaml)

*/

def call(releaseInfo, credentials = 'edgex-jenkins-github-personal-access-token') {
    validate(releaseInfo)
    createGitHubRelease(releaseInfo, credentials)
}

def validate(releaseInfo) {
    // raise error if releaseInfo map does not contain required attributes
    if(!releaseInfo.name) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'name'")
    }

    if(!releaseInfo.version) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'version'")
    }

    if(!releaseInfo.assets) {
        error("[edgeXReleaseGitHubAssets]: Release yaml does not contain 'assets'")
    }
}

def createGitHubRelease(releaseInfo, credentials) {
    def githubReleaseImage = 'nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest'
    def command = [
        'create-github-release',
        "--repo 'edgexfoundry/${releaseInfo.name}'",
        "--tag 'v${releaseInfo.version}'",
        "--assets 'assets/'"
    ]
    println "[edgeXReleaseGitHubAssets] command to run: ${command.join(' ')}"
    withCredentials([string(credentialsId: credentials, variable: 'GH_TOKEN_PSW')]) {
        docker.image(githubReleaseImage).inside {
            sh 'mkdir assets'
            releaseInfo.assets.each { asset ->
                sh "wget ${asset} -P assets/"
            }
            sh command.join(' ')
        }
    }
}