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

/**
 # edgeXSetupEnvironment

 ## Overview

 Shared library to setup a build environment given a Map of key value pairs. Some extra environment variables are set to help with the build process including:
 
 - `GIT_BRANCH`
 - `GIT_COMMIT`
 - `GIT_BRANCH_CLEAN`
 - `SHORT_GIT_COMMIT`
 - `SEMVER_BRANCH`
 - `SEMVER_PRE_PREFIX`

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 vars | false | str | A Map of key/value pairs to expose to the Jenkins environment |

 ## Usage

 ```groovy
 edgeXSetupEnvironment([ PROJECT: 'edgex-global-pipelines' ])
 ...
 ...
 // This will expose an environment variable named `PROJECT` with the value `edgex-global-pipelines`
 // as well as the extra environment vars mentioned above.
 ```
*/

def call(vars) {
    if(!vars) {
        vars = [:]
    }

    def gitEnvVars = ['GIT_BRANCH', 'GIT_COMMIT']
    gitEnvVars.each { var ->
        value = env."${var}"
        vars[var] = value

        if(var == 'GIT_BRANCH') {
            vars['SEMVER_BRANCH'] = value.replaceAll( /^origin\//, '' )
            vars['GIT_BRANCH_CLEAN'] = value.replaceAll('/', '_')
        }

        if(var == 'GIT_COMMIT') {
            vars['SHORT_GIT_COMMIT'] = value.substring(0, 7)
        }
    }

    vars['SEMVER_PRE_PREFIX'] = 'dev'

    vars.each { var, value ->
        println "[edgeXSetupEnvironment]: set envvar ${var} = ${value}"
        env."${var}" = value
    }
}