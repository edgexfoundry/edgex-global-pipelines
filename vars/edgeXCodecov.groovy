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
 # edgeXCodecov
 
 ## Overview

 Shared library to publish Codecov results to [CodeCov.io](https://codecov.io/edgexfoundry)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 tokenFile | false | str | Id of managed config file where token is stored. If `null` or empty, token file is automatically generated with: <br />`${env.PROJECT}-codecov-token` |

 ## Usage
 ```groovy
 edgeXCodecov('sample-service-codecov-token')
 ```
*/

def call(tokenFile = null) {
    if (tokenFile == null) {
        tokenFile = "${env.PROJECT}-codecov-token"
    }

    // See Jenkins example: https://docs.codecov.io/docs/supported-ci-providers
    configFileProvider([configFile(fileId: tokenFile, variable: "CODECOV_TOKEN_FILE")]) {
        sh "set +x ; export CODECOV_TOKEN=\$(cat ${env.CODECOV_TOKEN_FILE}) ; set -x ; curl -s https://codecov.io/bash | bash -s --"
    }
}