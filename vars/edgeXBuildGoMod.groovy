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
/**
 # edgeXBuildGoMod

 Shared Library to build Go projects using Go Modules. It invokes **edgeXBuildGoApp** with some default parameters.

 ## Parameters

 The parameters are similar to shared Library [edgeXBuildGoApp](https://edgexfoundry.github.io/edgex-global-pipelines/html/libraries/edgeXBuildGoApp/)
 with a few exceptions in default values of some parameters.
 ```
 buildImage: false,
 pushImage: false,
 semverBump: 'pre'
 ```
 ** Note:** These parameters are not overridable.

 ## Usage

 ### Basic example

 ```groovy
 edgeXBuildGoMod (
    project: 'go-mod-configuration'
 )
 ```

 ### Full example
 This example shows all the settings that can be specified and their default values.

 ```groovy
 edgeXBuildGoMod (
     project: 'go-project',
     mavenSettings: 'go-project-settings',
     semver: true,
     testScript: 'make test',
     buildScript: 'make build',
     goVersion: '1.16',
     goProxy: 'https://nexus3.edgexfoundry.org/repository/go-proxy/',
     useAlpineBase: true,
     dockerFilePath: 'Dockerfile',
     dockerBuildFilePath: 'Dockerfile.build',
     dockerBuildContext: '.',
     dockerBuildArgs: [],
     dockerNamespace: '',
     dockerImageName: 'docker-go-project',
     dockerNexusRepo: 'staging',
     buildImage: false,
     pushImage: false,
     semverBump: 'pre',
     buildSnap: false,
     publishSwaggerDocs: false,
     swaggerApiFolders: ['openapi/v1'],
     failureNotify: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
     buildExperimentalDockerImage: false,
     artifactTypes: ['docker'],
     artifactRoot: 'archives/bin',
     arch: ['amd64', 'arm64']
 )
 ```
 */
def call(config = [:]) {
    // per DevOps WG meeting on 12/3/20 go mod does not bump the patch version anymore
    def goModDefaults = [
        buildImage: false,
        pushImage: false,
        semverBump: 'pre'
    ]

    config << goModDefaults

    edgeXBuildGoApp(config)
}