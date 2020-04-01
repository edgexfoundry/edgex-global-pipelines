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

def call (releaseInfo) {
    validate(releaseInfo)
    publishDockerImages(releaseInfo)
}

@NonCPS
def getAvaliableTargets() {
    // this is the master list of valid hosts we will release to
    def validReleaseTargets = [
        'nexus3.edgexfoundry.org:10002': 'release',
        'docker.io': 'dockerhub'
    ]

    validReleaseTargets
}

@NonCPS
def isValidReleaseRegistry(targetImage) {
    def validHost = getAvaliableTargets()[targetImage.host]
    if(validHost && targetImage.host != 'docker.io') {
        true
    } else if(validHost && targetImage.host == 'docker.io' && 'edgexfoundry' == targetImage.namespace) {
        true
    } else {
        false
    }
}


// this method parses the releaseInfo information an maps dockerSource to dockerDestination
@NonCPS
def publishDockerImages (releaseInfo) {
    for(int i = 0; i < releaseInfo.dockerSource.size(); i++) {
        def dockerFrom = edgeXDocker.parse(releaseInfo.dockerSource[i])
        def publishCount = 0

        if(dockerFrom) {
            // set the source version to the releaseStream from the yaml
            dockerFrom.tag = releaseInfo.releaseStream

            for(int j = 0; j < releaseInfo.dockerDestination.size(); j++) {
                def dockerTo = edgeXDocker.parse(releaseInfo.dockerDestination[j])
                if(dockerTo) {
                    // set the destination version to the version from the yaml
                    dockerTo.tag = releaseInfo.version

                    // if we have matching image names...then publish the image
                    if(dockerFrom.imageName == dockerTo.imageName) {
                        if(isValidReleaseRegistry(dockerTo)) {
                            publishDockerImage (dockerFrom, dockerTo)
                            publishCount++
                        }
                    }
                }
            }

            if(publishCount == 0) {
                println "[edgeXReleaseDockerImage] The sourceImage [${releaseInfo.dockerSource[i]}] did not release...No cooresponding dockerDestination entry found."
            }
            else {
                println "[edgeXReleaseDockerImage] Successfully published [${publishCount}] images"
            }
        } else {
            println "[edgeXReleaseDockerImage] Could not parse docker source image: [${releaseInfo.dockerSource[i]}]"
        }
    }
}

@NonCPS
def publishDockerImage(from, to) {
    def finalFrom = edgeXDocker.toImageStr(from)
    def finalTo = edgeXDocker.toImageStr(to)

    if(finalTargetImage) {
        // for now assume edgeXDockerLogin() was already called...
        sh "echo docker tag ${finalFrom} ${finalTo}"
        sh "echo docker push ${finalTo}"
    }
}

@NonCPS
def validate(releaseYaml) {
    if(!releaseYaml.dockerSource) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerSource'")
    }

    if(!releaseYaml.dockerDestination) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerDestination'")
    }

    if(!releaseYaml.releaseStream) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain 'releaseStream' (branch where you are releasing from). Example: master")
    }

    if(!releaseYaml.version) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain release 'version'. Example: v1.1.2")
    }
}