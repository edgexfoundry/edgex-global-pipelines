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

def prepLTS(releaseInfo, options) {
    def credentials = options.credentials != null ? options.credentials : 'edgex-jenkins-ssh'
    edgeXReleaseGitTagUtil.validate(releaseInfo)
    def ltsBranchName = releaseInfo.releaseName

    // clone the repo branch to name using the specified ssh credentials
    println "[edgeXLTS]: Creating LTS Branch ${ltsBranchName} from ${releaseInfo.releaseStream}:${releaseInfo.commitId.take(7)} - DRY_RUN: ${env.DRY_RUN}"

    edgeXReleaseGitTag.cloneRepo(releaseInfo.repo, releaseInfo.releaseStream, ltsBranchName, releaseInfo.commitId, credentials)

    def ltsCommitId

    // Create LTS Branch
    sshagent(credentials: [credentials]) {
        if(edgex.isDryRun()) {
            echo("dir ${ltsBranchName}")
            echo("git checkout ${ltsBranchName} || git checkout -b ${ltsBranchName}")
        } else{
            dir(ltsBranchName) {
                sh "git checkout ${ltsBranchName} || git checkout -b ${ltsBranchName}"
            }
        }

        if (edgex.isGoProject(ltsBranchName)) {
            prepGoProject(ltsBranchName)
        }

        def commitMessage = generateLTSCommitMessage(releaseInfo.version, releaseInfo.commitId)

        if(edgex.isDryRun()) {
            echo("git commit --allow-empty -m '${commitMessage}'")
            echo('git rev-parse HEAD')
            echo("git push origin ${ltsBranchName}")
        } else {
            dir(ltsBranchName) {
                sh "git commit --allow-empty -m '${commitMessage}'"
                sh "git push origin ${ltsBranchName}"
                ltsCommitId = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            }
        }
    }

    ltsCommitId
}

def generateLTSCommitMessage(version, commitId) {
    "ci(lts-release): LTS release v${version} @${commitId.take(7)}"
}

def prepGoProject(ltsBranchName){
    if(edgex.isDryRun()) {
        println "[edgeXLTS]: Creating Vendored dependencies for Go project"

        echo("grep -v vendor .gitignore > .gitignore.tmp")
        echo("mv .gitignore.tmp .gitignore")
        echo("make vendor")
        echo("git add .")
    }
    else {
        dir("${ltsBranchName}") {
            sh "grep -v vendor .gitignore > .gitignore.tmp"
            sh "mv .gitignore.tmp .gitignore"

            def baseImage = edgex.getGoLangBaseImage('1.16', true)
            docker.image(baseImage).inside('-u 0:0') {
                sh 'make vendor'
            }
            sh "git add ."
        }
    }
}
