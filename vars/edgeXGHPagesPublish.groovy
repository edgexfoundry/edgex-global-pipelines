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

/**
 # edgeXGHPagesPublish

 ## Overview

 Shared library to publish html and other resources to a GitHub pages branch off the main repository (typically `gh-pages`).
 This shared library is typically used in conjunction with `mkdocs` and after `mkdocs` generates all the HTML, etc and the
 calling pipeline stashes the contents into a specific `site-contents` Jenkins stash.

 ## Process

 The typical documentation build process goes like this:
 
 - PR is merged into main in upstream repo
 - `mkdocs` is called to generate final documentation in upstream repo job.
 - `site-contents` stash is generated in upstream repo job.
 - `edgeXGHPagesPublish()` is called to publish stash to GitHub pages.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 repoUrl | true | str | Repo URL where GitHub pages are being published (typically in ssh format for Edgex).
 credentialId | false | str | Jenkins credentialId used to authenticate to git to push contents. <br /><br />**Default**: `edgex-jenkins-ssh`
 ghPagesBranch | false | str | Git branch where GitHub pages are stored. <br /><br />**Default**: `gh-pages`
 stashName | false | str | Stash name that contains generated site contents that will be published. <br /><br />**Default**: `site-contents`

 ## Usage
 
 ```groovy
 edgeXGHPagesPublish((repoUrl: 'git@github.com:edgexfoundry/edgex-docs.git')
 ```
*/

def call(Map config = [:]) {
    def dryRun         = ['1', 'true'].contains(env.DRY_RUN)
    def repoUrl        = config.repoUrl ?: null
    def credentialId   = config.credentialId ?: 'edgex-jenkins-ssh'
    def ghPagesBranch  = config.ghPagesBranch ?: 'gh-pages'
    def stashName      = config.stashName ?: 'site-contents'

    if (!repoUrl){
        error("[edgeXGHPagesPublish]: Repository URL missing in config")
    }

    try {
        def originalCommitMsg = sh(
            script: 'git log --format=%B -n 1 | grep -v Signed-off-by | head -n 1',
            returnStdout: true
        )

        // This is precaution to remove any files not needing to be published
        cleanWs()

        dir('gh-pages-src') {
            // clone repo we are publishing to
            git url: repoUrl, branch: ghPagesBranch,
                credentialsId: credentialId, changelog: false, poll: false

            unstash stashName

            withEnv([
                "DRY_RUN=${dryRun}",
                "GH_PAGES_BRANCH=${ghPagesBranch}",
                "COMMIT_MSG=${originalCommitMsg}"
            ]) {
                sshagent(credentials: [credentialId]) {
                    sh(script: libraryResource('github-pages-publish.sh'))
                }
            }
        }
    }
    catch (Exception ex){
        error("[edgeXGHPagesPublish]: ERROR occurred when publishing to GH Pages: ${ex}")
    }
}