//
// Copyright (c) 2019 Intel Corporation
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

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    
    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    def gitCheckoutExtensions = []

    if(config.tags) {
        gitCheckoutExtensions << 'tags'
    }

    if(config.lfs) {
        gitCheckoutExtensions << 'lfs'
    }

    def gitVars = null
    
    if(gitCheckoutExtensions) {
        def ex = []

        if(gitCheckoutExtensions.contains('tags')) {
            ex << [$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']
        }
        if(gitCheckoutExtensions.contains('lfs')) {
            ex << [$class: 'GitLFSPull']
        }

        gitVars = checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
            extensions: ex,
            userRemoteConfigs: scm.userRemoteConfigs,
        ])
    } else {
        gitVars = checkout scm
    }

    // setup git environment variables
    edgeXSetupEnvironment(gitVars)

    gitVars
}