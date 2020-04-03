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

releaseYaml:

version: 1.1.2
releaseStream: master
dockerSource:
  - nexus3.edgexfoundry.org:10004/sample-service
dockerDestination:
  - nexus3.edgexfoundry.org:10002/sample-service
  - docker.io/edgexfoundry/sample-service
---

edgeXReleaseDockerImage(releaseYaml)
)

*/
def call (releaseInfo) {
    validate(releaseInfo)
    publishDockerImages(releaseInfo)
}

def getAvaliableTargets() {
    // this is the master list of valid hosts we will release to
    def validReleaseTargets = [
        'nexus3.edgexfoundry.org:10002': 'release',
        'docker.io': 'dockerhub'
    ]

    validReleaseTargets
}

def isValidReleaseRegistry(targetImage) {
    def validHost = getAvaliableTargets()[targetImage.host]

    if(validHost && targetImage.host != 'docker.io') {
        println "[edgeXReleaseDockerImage] Valid Host: Nexus release detected."
        true
    } else if(validHost && targetImage.host == 'docker.io' && 'edgexfoundry' == targetImage.namespace) {
        println "[edgeXReleaseDockerImage] Valid Host: DockerHub release detected."
        true
    } else {
        println "[edgeXReleaseDockerImage] Invalid Host [${targetImage.host}] Unknown release detected."
        false
    }
}

// this method parses the releaseInfo information an maps dockerSource to dockerDestination
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
                echo "[edgeXReleaseDockerImage] The sourceImage [${dockerFrom ? edgeXDocker.toImageStr(dockerFrom) : releaseInfo.dockerSource[i] }] did not release...No corresponding dockerDestination entry found."
            }
            else {
                echo "[edgeXReleaseDockerImage] Successfully published [${publishCount}] images"
            }
        } else {
            echo "[edgeXReleaseDockerImage] Could not parse docker source image: [${releaseInfo.dockerSource[i]}]"
        }
    }
}

def publishDockerImage(from, to) {
    def finalFrom = edgeXDocker.toImageStr(from)
    def finalTo = edgeXDocker.toImageStr(to)

    if(finalFrom && finalTo) {
        // for now assume edgeXDockerLogin() was already called...
        def pullCmd = "docker pull ${finalFrom}"
        def tagCmd  = "docker tag ${finalFrom} ${finalTo}"
        def pushCmd = "docker push ${finalTo}"

        // default DRY_RUN is on (null)
        if([null, '1', 'true'].contains(env.DRY_RUN)) {
            echo([pullCmd, tagCmd, pushCmd].join('\n'))
        } else {
            sh pullCmd
            sh tagCmd
            sh pushCmd
        }
    }
}

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