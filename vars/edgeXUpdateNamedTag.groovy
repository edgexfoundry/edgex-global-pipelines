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

def call(ogVersion = null, namedVersion = null) {
    if(!ogVersion) {
        error('[edgeXUpdateNamedTag]: Original version (ogVersion) is required for the update named tag script.')
    }

    if(!namedVersion) {
        error('[edgeXUpdateNamedTag]: Named version (namedVersion) is required for the update named tag script.')
    }
    
    sshagent (credentials: ['edgex-jenkins-ssh']) {
        def tag_script = libraryResource('update-named-tag.sh')
    
        writeFile(file: './update-named-tag.sh', text: tag_script)

        sh "chmod +x ./update-named-tag.sh"
        sh "sudo chmod -R ug+w .git/*"
        sh "echo y | ./update-named-tag.sh v${ogVersion} ${namedVersion}"

        /* Commented out in the edgeXBuildGoApp we aren't setting env.DRY_RUN. 
        Likely need to set this in edgeXSetupEnvironment and the side effects of setting that is unknown.

        if(edgex.isDryRun()) {
            echo("echo y | ./update-named-tag.sh v${ogVersion} ${namedVersion}")
        } else {
            sh "echo y | ./update-named-tag.sh v${ogVersion} ${namedVersion}"
        }*/
    }
}