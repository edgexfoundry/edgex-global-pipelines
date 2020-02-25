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

def collectReleaseYamlFiles(filePath = 'release/*.yaml') {
    def releaseData = []
    def yamlFiles = findFiles(glob: filePath)

    for (f in yamlFiles) {
        if (edgex.didChange(f.toString())) {
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
        stage("${step.name}") {

            if(step.gitTag == true) {
                stage("Git Tag Publish") {

                    edgeXReleaseGitTag(step)

                }
            }
            
            if(step.dockerImages == true) {
                stage("Docker Image Publish") {
                    
                    edgeXReleaseDockerHub(step)
                    edgeXReleaseDockerNexus(step)

                }
            }

            if(step.snap == true) {
                stage("Snap Publish") {
                    
                    edgeXReleaseSnap(step)

                }
            }
        }
    }
}