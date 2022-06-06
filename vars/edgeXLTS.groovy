//
// Copyright (c) 2021 Intel Corporation
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
 # edgeXLTS

 ## Overview

 Shared library with helper functions to manage LTS releases. Used as part of the edgeXRelease.groovy pipeline.

 ## Functions:
 - `edgeXLTS.prepLTS`: Prepares a repository for an LTS release. If the repository is Golang based, go vendoring is enabled to support LTS releases.
 - `edgeXLTS.getLatestLTSCommitId`: Retrieves the latest LTS commit sha from a repository.
 - `edgeXLTS.generateLTSCommitMessage`: Creates a an LTS commit message for a release.
 - `edgeXLTS.prepGoProject`: Prepares a Golang based project for an LTS release enable vendoring and removing the `vendor` directory from the gitignore file.

*/

def prepLTS(releaseInfo, options) {
    def credentials = options.credentials != null ? options.credentials : 'edgex-jenkins-ssh'
    edgeXReleaseGitTagUtil.validate(releaseInfo)
    def ltsBranchName = releaseInfo.releaseName
    def dirName = "${releaseInfo.name}-${ltsBranchName}"

    // clone the repo branch to name using the specified ssh credentials
    println "[edgeXLTS]: Creating LTS Branch ${ltsBranchName} from ${releaseInfo.releaseStream}:${releaseInfo.commitId.take(7)} - DRY_RUN: ${env.DRY_RUN}"

    edgeXReleaseGitTag.cloneRepo(releaseInfo.repo, releaseInfo.releaseStream, dirName, releaseInfo.commitId, credentials)

    def ltsCommitId

    // Create LTS Branch
    sshagent(credentials: [credentials]) {
        if(edgex.isDryRun()) {
            echo("dir ${env.WORKSPACE}/${dirName}")
            echo("git checkout ${ltsBranchName} || git checkout -b ${ltsBranchName}")
        } else{
            dir(dirName) {
                sh "git checkout ${ltsBranchName} || git checkout -b ${ltsBranchName}"
            }
        }

        if (edgex.isGoProject(dirName)) {
            prepGoProject(dirName)
        }

        def commitMessage = generateLTSCommitMessage(releaseInfo.version, releaseInfo.commitId)

        if(edgex.isDryRun()) {
            echo("dir ${env.WORKSPACE}/${dirName}")
            echo("git commit --allow-empty -m '${commitMessage}'")
            echo('git rev-parse HEAD')
            echo("git push origin ${ltsBranchName}")
        } else {
            dir(dirName) {
                sh "git commit --allow-empty -m '${commitMessage}'"
                sh "git push origin ${ltsBranchName}"
                ltsCommitId = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            }
        }
    }

    ltsCommitId
}

// NOTE: This function will only work when run on an LTS branch
// This function is used in the C pipelines to determine the builder image tag to use
def getLatestLTSCommitId() {
    sh(script: 'git log --pretty="%H %s" | grep "ci(lts-release)" | head -n 1 | awk \'{print $1}\'', returnStdout: true).trim()
}

def generateLTSCommitMessage(version, commitId) {
    "ci(lts-release): LTS release v${version} @${commitId.take(7)}"
}

def prepGoProject(name){
    def goModVersion = edgex.getGoModVersion()

    if(edgex.isDryRun()) {
        println "[edgeXLTS]: Creating Vendored dependencies for Go project"

        echo("dir ${name}")
        echo("grep -v vendor .gitignore > .gitignore.tmp")
        echo("mv .gitignore.tmp .gitignore")
        echo("go version to be used: ${goModVersion}")
        echo("make vendor")
        echo("git add .")
    }
    else {
        dir(name) {
            sh "grep -v vendor .gitignore > .gitignore.tmp"
            sh "mv .gitignore.tmp .gitignore"

            def baseImage = edgex.getGoLangBaseImage(goModVersion, true)
            docker.image(baseImage).inside('-u 0:0') {
                sh 'make vendor'
            }
            sh "git add ."
        }
    }
}
