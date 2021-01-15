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
            if(step.gitTag == true) {
                stage("Git Tag Publish") {
                    edgeXReleaseGitTag(step, [credentials: "edgex-jenkins-ssh", bump: false, tag: true])
                }
                stage("Stage Artifact") {
                    stageArtifact(step)
                }
                stage("Bump Semver") {
                    edgeXReleaseGitTag(step, [credentials: "edgex-jenkins-ssh", bump: true, tag: false])
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
        }
    }
}

def stageArtifact(step) {
    rebuildRepo = step.repo.split('/')[-1].split('.git')[0]
    println "[edgeXRelease]: building/staging for ${rebuildRepo} - DRY_RUN: ${env.DRY_RUN}"
    if(edgex.isDryRun()) {
        println("build job: '../${rebuildRepo}/${step.releaseStream}, parameters: [CommitId: ${step.commitId}], propagate: true, wait: true)")
    }
    else {
        build(
            job: '../' + rebuildRepo + '/' + step.releaseStream,
            parameters: [[$class: 'StringParameterValue', name: 'CommitId', value: step.commitId]],
            propagate: true,
            wait: true)
    }
}