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
def call (config) {
    def _branchName = config.branchName ?: 'master'
    def _version = config.version ?: 'v0.0.1-test'
    def _dockerNexusURL = config.dockerNexusURL ?: 'nexus3edgexfoundry.org'
    def _dockerImageRepo = config.dockerImageRepo ?: null
    def _from = config.from ?: 'staging'
    def _to = config.to ?: 'release'
    def nexusPortMapping = [
        snapshots: 10003,
        snapshot: 10003,
        staging: 10004,
        release: 10002
    ]
    def sourceNexusPort = nexusPortMapping[_from]
    def destinationNexusPort = nexusPortMapping[_to]
    if(!_dockerImageRepo) {
        throw new Exception('Docker image repository (dockerImageRepo) is required.')
    }
    // Pull image from nexus soure repository
    sh "docker pull ${_dockerNexusURL}:${sourceNexusPort}/${_dockerImageRepo}:${_branchName}"
    // Get image ID from pulled image
    def imageID = sh(script: "docker image -q ${_dockerImageRepo}:${_branchName}" returnStdout: true)
    // Retag pulled image
    sh "docker tag ${imageID} ${_dockerNexusURL}:${destinationNexusPort}/${_dockerImageRepo}:${_version}"
    // Push new image
    sh "docker push ${_dockerNexusURL}:${destinationNexusPort}/${_dockerImageRepo}:${_version}"
}