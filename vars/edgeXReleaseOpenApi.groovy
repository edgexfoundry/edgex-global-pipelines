//
// Copyright (c) 2022 Intel Corporation
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
 # edgeXReleaseOpenApi

 ## Overview

 Shared library with helper functions to manage OpenAPI YAML related changes. Currently used by sdk repos.

 ## Required Yaml

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 apiInfo.nextReleaseVersion | true | str | Next release version to for the OpenAPI yaml files. |
 apiInfo.reviewers | true | str | Who to assign the generated PR to. |

 ## Functions
 - `edgeXReleaseOpenApi.publishReleaseBranch`: Makes release branch related changes in unique branch then commits release branch.
 - `edgeXReleaseOpenApi.publishOpenApiChanges`: Makes OpenAPI related changes in unique branch then commits and opens PR.
 - `edgeXReleaseOpenApi.validate`: Validates release yaml input before any automation is run.
 
 ## Usage

 ### Sample Release Yaml

 ```yaml
 name: 'device-sdk-go'
 version: '2.2.0'
 releaseName: 'kamakura'
 releaseStream: 'main'
 repo: 'https://github.com/edgexfoundry/device-sdk-go.git'
 commitId: 'c72b16708d6eed9a08be464a432ce22db7d90667'
 gitTag: true
 dockerImages: false
 gitHubRelease: false
 apiInfo: [
    nextReleaseVersion: '2.3.0',
    reviewers: edgexfoundry/edgex-committers
 ]
 ```

 # Groovy Call

 ```groovy
 edgeXReleaseOpenApi(releaseYaml)
 ```
*/

// For this function DRY_RUN is treated differently. For dry run here
// it is helpful to see all the modification steps happening with the
// exception of actually pushing the changes or opening a PR.
def call (releaseInfo) {
    // Check to see if we have already cloned the repo
    if(!fileExists(releaseInfo.name)) {
        withEnv(['DRY_RUN=false']) {
            edgeXReleaseGitTag.cloneRepo(
                releaseInfo.repo,
                releaseInfo.releaseStream,
                releaseInfo.name,
                releaseInfo.commitId,
                'edgex-jenkins-ssh'
            )
        }
    }

    // set user info for commits
    sh 'git config --global user.email "jenkins@edgexfoundry.org"'
    sh 'git config --global user.name "EdgeX Jenkins"'

    dir(releaseInfo.name) {
        if(fileExists('openapi/v2')) {
            // Adding stage in here as detection is based on files in the workspace
            stage("OpenApi Version Bump") {
                echo '[edgeXReleaseOpenApi] Detected openapi/v2 folder. Validating release yaml.'
                validate(releaseInfo)
                publishOpenApiChanges(releaseInfo)
            }
        }
        else {
            echo '[edgeXReleaseOpenApi] No OpenApi Yaml to bump. Doing nothing.'
        }
    }
}

def publishOpenApiChanges(releaseInfo) {
    def branch = "${releaseInfo.releaseName}-openapi-version-changes"
    sh "git reset --hard ${releaseInfo.commitId}"
    sh "git checkout -b ${branch}"

    def nextApiVersion = releaseInfo.apiInfo.nextReleaseVersion

    sh "sed -E -i 's|  version: (.*)|  version: ${nextApiVersion}|g' openapi/v2/*.yaml"

    edgex.bannerMessage "[edgeXReleaseOpenApi] Here is the diff related release branch changes"
    sh 'git diff'

    def title = "ci: automated version changes for OpenAPI version: [${nextApiVersion}]"
    def body = "This PR updates the OpenAPI version yaml the next release version ${nextApiVersion}"

    edgex.createPR(branch, title, body, releaseInfo.apiInfo.reviewers)
}

def validate(releaseYaml) {
    if(!releaseYaml.apiInfo) {
        error("[edgeXReleaseOpenApi] Release yaml does not contain 'apiInfo' block. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
    }
    else {
        if(!releaseYaml.apiInfo.nextReleaseVersion) {
            error("[edgeXReleaseOpenApi] Release yaml does not contain 'nextReleaseVersion'. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
        }

        if(!releaseYaml.apiInfo.reviewers) {
            error("[edgeXReleaseOpenApi] Release yaml does not contain 'reviewers'. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
        }
    }

}
