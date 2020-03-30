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
// Define parameters

/*

version: 1.1.2
dockerSource:
  - nexus3.edgexfoundry.org:10004/sample-service:master
dockerDestination:
  - nexus3.edgexfoundry.org:10002/sample-service
  - docker.io/edgexfoundry/sample-service
---

edgeXReleaseDockerImage(
    from: 'nexus3.edgexfoundry.org:10004/sample-service:master',
    to: 'edgexfoundry/sample-service',
    version: 'v0.0.1-test'
)

*/
import com.cloudbees.groovy.cps.NonCPS

def call (config) {
    validate(config)

    def _sourceImage = config.from
    def _releaseTarget = config.to
    def _version = config.version

    def _releaseTargetRepo = getReleaseTarget(_releaseTarget)
    
    if(_releaseTargetRepo) {
        def finalTargetImage = getFinalImageWithTag(_sourceImage, _releaseTargetRepo, _version)

        if(finalTargetImage) {
            // for now assume edgeXDockerLogin() was already called...

            echo "======================================================="
            echo "docker tag ${_sourceImage} ${finalTargetImage}"
            echo "docker push ${finalTargetImage}"
            echo "======================================================="
        }

    } else {
        error("Unknown release target: Available targets: ${getAvaliableTargets().collect{ k,v -> v }.join(', ') }")
    }
}

@NonCPS
def getFinalImageWithTag(sourceImage, releaseTargetRepo, version) {
    def fullImageName = sourceImage.substring(sourceImage.indexOf('/')+1) //edgex-devops/sample-service
    def finalTargetImage = "${releaseTargetRepo}/${fullImageName.split(':')[0]}:${version}"

    finalTargetImage
}

def validate(config) {
    if(!config.from) {
        error("[edgeXReleaseDockerImage] Please provide source image. Example: from: 'nexus3.edgexfoundry.org:10004/sample:master'")
    }

    if(!config.to) {
        error("[edgeXReleaseDockerImage] Please provide release target: Available targets: ${getAvaliableTargets().collect{ k,v -> v }.join(', ') }")
    }

    if(!config.version) {
        error("[edgeXReleaseDockerImage] Please provide release version. Example: v1.1.2")
    }
}

@NonCPS
def getAvaliableTargets() {
    def validReleaseTargets = [
        'nexus3.edgexfoundry.org:10002': 'release',
        'docker.io': 'dockerhub'
    ]

    validReleaseTargets
}

@NonCPS
def getReleaseTarget(targetImage) {
    def targetHost = targetImage.replaceAll('https://', '').split('/')[0]
    def validHost = getAvaliableTargets()[targetHost]

    def dockerHubHost = 'docker.io'
    def dockerHubNamespace = 'edgexfoundry'

    // handle both uses cases where user passes in
    // docker.io/edgexfoundry/foo
    // edgexfoundry/foo

    if(validHost == 'dockerhub' || (!validHost && targetHost == dockerHubNamespace)) {
        def searchIndex = !validHost ? 0 : 1
        def ns = targetImage.split('/')[searchIndex]
        if(dockerHubNamespace == ns) {
            "${dockerHubHost}/${dockerHubNamespace}"
        }
        else {
            null
        }
    } else if(validHost) {
        targetHost
    } else {
        null
    }
}