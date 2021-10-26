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
releaseStream: main
dockerImage: true
docker:
  - image: nexus3.edgexfoundry.org:10004/sample-service
    destination:
      - nexus3.edgexfoundry.org:10002/sample-service
      - docker.io/edgexfoundry/sample-service
  - image: nexus3.edgexfoundry.org:10004/sample-service-arm64
    destination:
      - nexus3.edgexfoundry.org:10002/sample-service-arm64
      - docker.io/edgexfoundry/sample-service-arm64
---

edgeXReleaseDockerImage(releaseYaml)
)

*/
def call (releaseInfo) {
    validate(releaseInfo)
    publishDockerImages(releaseInfo)
}

def getAvaliableTargets() {
    // this is the main list of valid hosts we will release to
    def validReleaseTargets = [
        'nexus3.edgexfoundry.org:10002': 'release',
        'docker.io': 'dockerhub'
    ]

    validReleaseTargets
}

def isValidReleaseRegistry(targetImage) {
    def validHost = getAvaliableTargets()[targetImage.host]

    if(validHost && targetImage.host != 'docker.io') {
        // println "[edgeXReleaseDockerImage] Valid Host: Nexus release detected."
        true
    } else if(validHost && targetImage.host == 'docker.io' && 'edgexfoundry' == targetImage.namespace) {
        // println "[edgeXReleaseDockerImage] Valid Host: DockerHub release detected."
        true
    } else {
        // println "[edgeXReleaseDockerImage] Invalid Host [${targetImage.host}] Unknown release detected."
        false
    }
}

// this method parses the releaseInfo information an maps docker element in the yaml
def publishDockerImages (releaseInfo) {
    if(edgex.isDryRun()) {
        echo "[edgeXReleaseDockerImage] DRY_RUN: docker login happens here"
    } else {
        edgeXDockerLogin(settingsFile: env.RELEASE_DOCKER_SETTINGS)
    }

    for(int i = 0; i < releaseInfo.docker.size(); i++) {
        def dockerInfo = releaseInfo.docker[i]

        def dockerFrom = edgeXDocker.parse(dockerInfo.image)
        def publishCount = 0

        if(dockerFrom) {
            // set the source version to the releaseStream from the yaml
            def sourceTag = releaseInfo.lts ? releaseInfo.releaseName : releaseInfo.releaseStream
            dockerFrom.tag = sourceTag

            for(int j = 0; j < dockerInfo.destination.size(); j++) {
                def dockerTo = edgeXDocker.parse(dockerInfo.destination[j])
                if(dockerTo) {
                    // set the destination version to the version from the yaml
                    dockerTo.tag = releaseInfo.version

                    def exists = false
                    if(releaseInfo.lts) {
                        try {
                            exists = imageExists(dockerTo)
                        } catch (e) {
                            prinln "[edgeXReleaseDockerImage] LTS Release. Destination image does not exist ignoring error."
                            exists = false
                        }
                    } else {
                        exists = imageExists(dockerTo)
                    }

                    // if destination image doesn't exit or we are forcing it, go ahead and publish.
                    if (!exists || releaseInfo.dockerForce) {
                        // if we have matching image names...then publish the image
                        if(isValidReleaseRegistry(dockerTo)) {
                            publishDockerImage (dockerFrom, dockerTo)
                            publishCount++
                        }
                    } else {
                        echo "[edgeXReleaseDockerImage] Image exists at destination and option 'dockerForce' was not set in release.yaml file. [${dockerTo ? edgeXDocker.toImageStr(dockerTo) : dockerInfo.image}] did not release..."
                        unstable(message: 'A Docker Image did not release')
                    }
                }
            }

            if(publishCount == 0) {
                echo "[edgeXReleaseDockerImage] The sourceImage [${dockerFrom ? edgeXDocker.toImageStr(dockerFrom) : dockerInfo.image}] did not release..."
            }
            else {
                echo "[edgeXReleaseDockerImage] Successfully published [${publishCount}] images"
            }
        } else {
            echo "[edgeXReleaseDockerImage] Could not parse docker source image: [${dockerInfo.image}]"
        }
    }
}

def publishDockerImage(from, to) {
    def finalFrom = edgeXDocker.toImageStr(from)
    def finalTo = edgeXDocker.toImageStr(to)

    if(finalFrom && finalTo) {
        def pullCmd = "docker pull ${finalFrom}"
        def tagCmd  = "docker tag ${finalFrom} ${finalTo}"
        def pushCmd = "docker push ${finalTo}"

        if(edgex.isDryRun()) {
            if(env.DRY_RUN_PULL_DOCKER_IMAGES && env.DRY_RUN_PULL_DOCKER_IMAGES == 'true') {
                sh pullCmd
            }
            echo([pullCmd, tagCmd, pushCmd].join('\n'))
        } else {
            sh pullCmd
            sh tagCmd
            sh pushCmd
        }
    }
}

def validate(releaseYaml) {
    if(!releaseYaml.docker) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain a list 'docker' images")
    }

    if(!releaseYaml.releaseStream) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain 'releaseStream' (branch where you are releasing from). Example: main")
    }

    if(!releaseYaml.version) {
        error("[edgeXReleaseDockerImage] Release yaml does not contain release 'version'. Example: v1.1.2")
    }
}

def imageExists(image) {
    sh(script: "docker pull ${edgeXDocker.toImageStr(image)} > /dev/null && exit 1 || exit 0", returnStatus: true)
}