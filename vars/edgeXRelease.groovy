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
 # edgeXRelease

 ## Overview

 Shared library with helper functions to manage releases. This is the main entry point for all automated releases using the 🔗 [cd-management/release](https://github.com/edgexfoundry/cd-management/blob/release/Jenkinsfile) repository.

 ## Required Yaml

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 name | true | str | Name of the repository that is being released. |
 version | true | str | Semver version to be released. |
 releaseName | true | str | The codename of the release. This is usually used by sub release processes for naming. |
 releaseStream | true | str | What branch the release is being generated from. (This has been superseded by commitId) |
 commitId | true | str | Git Commit SHA to tag or branch for the release. |
 repo | true | str | https git repository url. <br />**Example:** `https://github.com/edgexfoundry/<repo>.git` |

 ## Functions
 - `edgeXRelease.collectReleaseYamlFiles`: Search through a provided file path and find all the release yaml files to process and parses yaml into a Groovy object using the `readYaml` function. Default file pattern to search for will be: `release/*.yaml`. This function will also validate what files have changed in the commit using `edgex.didChange` and will only release what was changed.
 - `edgeXRelease.parallelStepFactory`: Returns a Closure to execute the release for all release yaml entries found for the specific commit.
 - `edgeXRelease.parallelStepFactoryTransform`: Transforms release yaml Groovy object into a Groovy Closure containing all the logic needed to perform the release for the specific repository.
 - `edgeXRelease.stageArtifact`: If release contains binaries rather than docker images, the binaries need to be staged before the release occurs. This function forces a build of the artifact by triggering the job using the `build(job: ..)` Jenkins function.
 - `edgeXRelease.getBuilderImagesFromReleasedImages`: Used to determine what static builder image to use when building a LTS C based repository. Below you can see the transformation.
   ```groovy
    // Given the release of the following 2 docker images:
    edgeXRelease.getBuilderImagesFromReleasedImages('nexus3.edgexfoundry.org:10004/sample-service-c')
    > nexus3.edgexfoundry.org:10002/sample-service-c-builder-x86_64:jakarta

    edgeXRelease.getBuilderImagesFromReleasedImages('nexus3.edgexfoundry.org:10004/sample-service-c-arm64')
    > nexus3.edgexfoundry.org:10002/sample-service-c-builder-arm64:jakarta
   ```
*/

def collectReleaseYamlFiles(filePath = 'release/*.yaml', releaseBranch = 'release') {
    def releaseData = []
    def yamlFiles = findFiles(glob: filePath)

    for (f in yamlFiles) {
        if (edgex.didChange(f.toString(), releaseBranch)) {
            releaseData << readYaml(file: f.toString())
        }
    }

    return releaseData
}

def parallelStepFactory(data) {
    return data.collectEntries {
        ["${it.name}" : parallelStepFactoryTransform(it)]
    }
}

def parallelStepFactoryTransform(step) {
    return {
        stage(step.name.toString()) {
            if (step.lts == true){
                stage("LTS Prep") {
                    def ltsCommitId = edgeXLTS.prepLTS(step, [credentials: "edgex-jenkins-ssh"])

                    if(edgex.isDryRun()) {
                        println "[edgeXRelease] Will override CommitId [${step.commitId}] with new CommitId from edgeXLTS.prepLTS"
                        ltsCommitId = 'mock-lts-build'
                    } else {
                        println "[edgeXRelease] New CommitId created for LTS Release [${ltsCommitId}] overriding step CommitId [${step.commitId}]"
                    }

                    step.commitId = ltsCommitId

                    if (step.name.toString().matches(".*-c\$")){
                        def images = getBuilderImagesFromReleasedImages(step, ltsCommitId)

                        println("[edgeXRelease] Wait for these Images: ${images}")
                        if(!edgex.isDryRun()) {
                            edgex.waitForImages(images, 30)

                            // Adding extra sleep. Have seen issues where images are not "available" after push happens
                            sh 'sleep 15'
                        }
                    }
                }
            }

            if(step.gitTag == true) {
                stage("Git Tag Publish") {
                    edgeXReleaseGitTag(step, [credentials: "edgex-jenkins-ssh", bump: false, tag: true])
                }
                try{
                    stage("Stage Artifact") {
                        stageArtifact(step)
                    }
                }finally {
                    stage("Bump Semver"){
                        edgeXReleaseGitTag(step, [credentials: "edgex-jenkins-ssh", bump: true, tag: false])
                    }
                }
            }
            
            if(step.dockerImages == true) {
                stage("Docker Image Publish") {
                    edgeXReleaseDockerImage(step)
                }
            }

            if(step.gitHubRelease == true) {
                stage("GitHub Release") {
                    edgeXReleaseGitHubAssets(step)
                }
            }

            if(step.docs == true) {
                stage("Docs Release") {
                    edgeXReleaseDocs(step)
                }
            }
        }
    }
}

def stageArtifact(step) {
    rebuildRepo = step.repo.split('/')[-1].split('.git')[0]

    println "[edgeXRelease]: building/staging for ${rebuildRepo} - DRY_RUN: ${env.DRY_RUN}"

    def jobToBuild = step.lts ? step.releaseName : step.releaseStream

    if(edgex.isDryRun()) {
        println("build job: '../${rebuildRepo}/${jobToBuild}, parameters: [CommitId: ${step.commitId}], propagate: true, wait: true)")
    } else {
        try{
            build(job: "../${rebuildRepo}/${jobToBuild}",
                parameters: [[$class: 'StringParameterValue', name: 'CommitId', value: step.commitId]],
                propagate: true,
                wait: true
            )
        } catch (hudson.AbortException e) {
            if (e.message.matches("No item named(.*)found")){
                catchError(stageResult: 'UNSTABLE', buildResult: 'SUCCESS'){
                    error ('[edgeXRelease]: No build pipeline found - No artifact to stage')
                }
            } else{
                throw e
            }
        }
    }
}

def getBuilderImagesFromReleasedImages(step, tag) {
    // TODO: allow configuration of some of these hardcoded vars
    def releaseRegistry = 'nexus3.edgexfoundry.org:10002'
    def x86Tag = 'x86_64'
    def armTag = 'arm64'

    def builderImages = []

    def builderImageName = step.name

    if(builderImageName == 'device-sdk-c') {
        builderImages << "${releaseRegistry}/${builderImageName}-builder-${x86Tag}:${tag}"
        builderImages << "${releaseRegistry}/${builderImageName}-builder-${armTag}:${tag}"
    } else {
        step.docker.each {
            def imageName = it.image.split("/")[-1]
            if (imageName =~ /.*-arm64$/) {
                builderImages << "${releaseRegistry}/${builderImageName}-builder-${armTag}:${tag}"
            } else {
                builderImages << "${releaseRegistry}/${builderImageName}-builder-${x86Tag}:${tag}"
            }
        }
    }

    return builderImages
}