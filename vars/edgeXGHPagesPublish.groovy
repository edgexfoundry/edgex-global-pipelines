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

def call() {
    cleanWs()
    dir('edgex-docs-clean') {
        git url: 'git@github.com:edgexfoundry/edgex-global-pipelines.git', branch: 'gh-pages', credentialsId: 'edgex-jenkins-ssh', changelog: false, poll: false
        unstash 'site-contents'
        sshagent(credentials: ['edgex-jenkins-ssh']) {
            sh './scripts/github-pages.sh'
        }
    }
}