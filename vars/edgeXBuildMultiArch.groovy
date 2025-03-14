// Copyright (c) 2025 IOTech Ltd.

/**
 # edgeXBuildMultiArch

 ## Overview

 Shared Library to build multi-arch docker images

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 settingsFile | true | str | Config file Id that contains authentication details to docker registries. Unique to each Edgex repository. |
 images | true | list | This list of existing images to re-tag and build multi-arch images. |

 ## Usage

 ```groovy
 edgeXBuildMultiArch(images: ["nexus3.edgexfoundry.org:10004/sample-service:latest"], settingsFile: 'edgex-repo-settings')
 ```
*/

def call(config = [:]) {
    edgex.bannerMessage "[edgeXBuildMultiArch] RAW Config: ${config}"
    def images = config.images
    if(!images) {
        error('[edgeXBuildMultiArch] Images list (images) is required.')
    }
    def settingsFile = config.settingsFile
    if(!settingsFile) {
        error('[edgeXBuildMultiArch] Project Settings File id (settingsFile) is required.')
    }
    node('centos7-docker-4c-2g'){
        try {
            stage('Create Multi-Arch Images'){
                edgeXDockerLogin(settingsFile: settingsFile)
                bootstrapBuildX()
                images.each { image ->
                    sh "echo -e 'FROM ${image}' | docker buildx build --platform 'linux/amd64,linux/arm64' -t ${image} --push -"
                }
            }
        } catch(err){
            error('[edgeXBuildMultiArch] Error details: ${err.message}')
        }
        finally{
            edgeXInfraPublish()
            cleanWs()
        }
    }
}

def bootstrapBuildX() {
    sh 'docker buildx ls'
    sh 'docker buildx create --name edgex-builder --platform linux/amd64,linux/arm64 --use'
    sh 'docker buildx inspect --bootstrap'
    sh 'docker buildx ls'
}
