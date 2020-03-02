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

def call(dockerImage = null, dockerFile = null) {
    def snykImage = 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-snyk-go:1.217.3'
    def org = env.SNYK_ORG ?: 'edgex-jenkins'

    // Run snyk monitor by default
    def command = ['snyk', 'monitor', "--org=${org}"]

    println "[edgeXSnyk] dockerImage=${dockerImage}, dockerFile=${dockerFile}"
    
    // If docker specified alter test command
    if(dockerImage != null && dockerFile != null) {
        command << "--docker ${dockerImage}"
        command << "--file=./${dockerFile}"
    }

    println "[edgeXSnyk] command to run: ${command.join(' ')}"

    withCredentials([string(credentialsId: 'snyk-cli-token', variable: 'SNYK_TOKEN')]) {
        docker.image(snykImage).inside("-u 0:0 --privileged -v /var/run/docker.sock:/var/run/docker.sock -v ${env.WORKSPACE}:/ws -w /ws --entrypoint=''") {
            sh command.join(' ')
        }
    }
}