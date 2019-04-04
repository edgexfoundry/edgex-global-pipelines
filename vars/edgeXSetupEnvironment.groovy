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

def call(vars) {
    if(vars != null) {
        vars.each { k, v ->
            env.setProperty(k, v)
            if(k == 'GIT_BRANCH') {
                env.setProperty('SEMVER_BRANCH', v.replaceAll( /^origin\//, '' ))
                env.setProperty('GIT_BRANCH_CLEAN', v.replaceAll('/', '_'))
            } else if(k == 'GIT_COMMIT') {
                env.setProperty('SHORT_GIT_COMMIT', env.GIT_COMMIT.substring(0,7))
            }
        }
    }

    // attempty to set a default architecture
    if(!env.ARCH) {
        def vmArch = sh(script: 'uname -m', returnStdout: true).trim()
        env.setProperty('ARCH', vmArch)
    }
}