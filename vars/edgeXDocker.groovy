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
 # edgeXDocker

 ## Overview

 Shared library that contains convenience functions for interacting with Docker. This shared library contains numerous functions so only a summary will be provided for each given function.
 If you have further questions about implementation details please refer to the [source code](https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgeXDocker.groovy).
 
 ## Functions:
 - `edgeXDocker.build`: Build a docker image from optional `baseImage` with a set of default: docker build_args, labels, and tags
 - `edgeXDocker.buildInParallel`: Build multiple docker images in parallel. This technique utilizes docker-compose to build multiple images using the parallel flag.
 - `edgeXDocker.generateDockerComposeForBuild`: Supporting function for `edgeXDocker.buildInParallel` that generates a docker compose file for a given list of docker images.
 - `edgeXDocker.generateServiceYaml`: Supporting function for `edgeXDocker.buildInParallel` that generates service level yaml for a specific docker image.
 - `edgeXDocker.push`: Push a specific docker image and optionally tag it with the `latest` tag. A nexus repository can also optionally be specified as well as specific tags.
 - `edgeXDocker.pushAll`: Push all docker images specified in the `dockerImages` list. To be used in conjunction with the same input format used by `edgeXDocker.buildInParallel` to push all images.
 - `edgeXDocker.getDockerTags`: Generates the default set of tags used when pushing all edgex docker images with the `edgeXDocker.push` function.
 - `edgeXDocker.finalImageName`: Prepends a docker image with `env.DOCKER_REGISTRY_NAMESPACE` if defined.
 - `edgeXDocker.cleanImageUrl`: Returns image url without protocol
 - `edgeXDocker.parse`: Reads a docker image url and returns the parsed [image object](#parsed-image-object) components
 - `edgeXDocker.toImageStr`: Returns docker image string from an [image object](#parsed-image-object)

 ## Parsed image object

 ```groovy
 [
    host: hostname if any associated with the image,
    fullImage: full image name with tag,
    namespace: namespace of the image if any,
    image: image name without tag,
    tag: tag associated with the image if any
 ]
 ```
*/

def build(dockerImageName, baseImage = null) {
    def buildArgs = ['']

    if(baseImage) {
        buildArgs << "BASE='${baseImage}'"
    }

    if(env.BUILD_SCRIPT) {
        buildArgs << "MAKE='${env.BUILD_SCRIPT}'"
    }

    // transform to standard arch
    def buildArgArch = env.ARCH

    if(env.ARCH == 'x86_64') {
        buildArgArch = 'amd64'
    } else if(env.ARCH == 'aarch64') {
        buildArgArch = 'arm64'
    }

    buildArgs << "ARCH=${buildArgArch}"

    if(env.http_proxy) {
        buildArgs << "http_proxy"
        buildArgs << "https_proxy"
    }

    if(env.DOCKER_BUILD_ARGS) {
        env.DOCKER_BUILD_ARGS.split(',').each { buildArgs << it }
    }

    def buildArgString = buildArgs.join(' --build-arg ')

    def labels = ['', "'git_sha=${env.GIT_COMMIT}'", "'arch=${buildArgArch}'"]

    // in case VERSION is not set (i.e. use semver = false)
    if(env.VERSION) {
        labels << "'version=${env.VERSION}'"
    }

    // devops labels go here (date, branch, build number, etc)
    def labelString = labels.join(' --label ')

    // for LF jenkins, reset owner before build
    sh 'sudo chown -R jenkins:jenkins .'
    sh 'ls -al .'

    docker.build(finalImageName(dockerImageName), "-f ${env.DOCKER_FILE_PATH} ${buildArgString} ${labelString} ${env.DOCKER_BUILD_CONTEXT}")
}

def buildInParallel(dockerImages, imageNamePrefix, baseImage = null) {
    //check if docker-compose --parallel is available
    def composeImage = env.ARCH == 'arm64'
        ? 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose-arm64:latest'
        : 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest'

    def parallelSupported = -1

    docker.image(composeImage).inside('--entrypoint=') {
        parallelSupported = sh(script: 'docker-compose build --help | grep parallel', returnStatus: true)
    }

    if(parallelSupported == 0) {
        def labels = [
            git_sha: env.GIT_COMMIT,
            arch: env.ARCH
        ]

        if(env.VERSION) {
            labels << [version: env.VERSION]
        }

        // generate ephemeral docker-compose based on docker image name and dockerfile
        def dockerCompose = generateDockerComposeForBuild(dockerImages, labels, imageNamePrefix, env.ARCH)

        // write ephemeral docker-compose file
        writeFile(file: './docker-compose-build.yml', text: dockerCompose)

        // always set builder base in case it is not setup in the user's Jenkinsfile
        def envVars = baseImage ? ["BUILDER_BASE=${baseImage}"] : []

        withEnv(envVars) {
            docker.image(composeImage).inside('-u 0:0 --entrypoint= -v /var/run/docker.sock:/var/run/docker.sock --privileged') {
                sh 'docker-compose -f ./docker-compose-build.yml build --parallel'
            }

            sh 'docker images' //debug
        }
    } else {
        error '[edgeXDocker] --parallel build is not supported in this version of docker-compose'
    }
}

def generateDockerComposeForBuild(services, labels, imageNamePrefix = '', arch = null) {
"""
version: '3.7'
services:
${services.collect { generateServiceYaml(it.image, imageNamePrefix, it.dockerfile, labels, arch) }.join('\n') }
"""
}

def generateServiceYaml(serviceName, imageNamePrefix, dockerFile, labels, arch = null) {
    if(imageNamePrefix == null) {
        imageNamePrefix = ''
    }

    def labelStr = labels ? 'labels:\n' + labels.collect { k,v -> "        - ${k}=${v}"}.join('\n') : ''
    def imageNameSuffix = arch && arch == 'arm64' ? "-${arch}" : ''
"""
  ${serviceName}:
    build:
      context: .
      dockerfile: ${dockerFile}
      ${labelStr}
      args:
        - BUILDER_BASE
    image: ${imageNamePrefix}${serviceName}${imageNameSuffix}"""
}


// overload dockerImage to be a String or a Map
def push(dockerImage, latest = true, nexusRepo = 'staging', tags = null) {
    def taggedImages = []

    def nexusPortMapping = [
        snapshots: 10003,
        snapshot: 10003,
        staging: 10004,
        release: 10002
    ]

    def nexusPort = nexusPortMapping[nexusRepo]

    if(tags == null) {
        tags = getDockerTags(latest)
    }

    def finalImage = dockerImage

    // I am not sure I like this :(
    // use case whe dockerImage is a string like "docker-edgex-foo"
    if(dockerImage.class.name =~ /String/) {
        def defaultRegistry = env.DOCKER_REGISTRY ?: 'nexus3.edgexfoundry.org'
        finalImage = parse(finalImageName(dockerImage), false)
        finalImage.host = "${defaultRegistry}:${nexusPort}"
    }

    def image = docker.image(finalImageName(finalImage.image))

    println """[edgeXDocker.push] Tagging docker image ${finalImage.image} with the following tags:
${tags.join('\n')}
====================================================="""

    def registry = "https://${finalImage.host}"
    if(finalImage.namespace) {
        registry += "/${finalImage.namespace}"
    }

    docker.withRegistry(registry) {
        tags.each {
            image.push(it)
            finalImage.tag = it
            taggedImages << "${toImageStr(finalImage)}"
        }
    }
    println "=====================================================" //debug
    println "taggedImages:\n${taggedImages.collect {"  - ${it}"}.join('\n')}" //debug

    taggedImages
}

/*
   dockerImages = [
       [image: 'docker-example', dockerfile: '/path/to/dockerfile'],
       ...
   ]
*/
def pushAll(dockerImages, latest = true, nexusRepo = 'staging', arch = null) {
    def pushedImages = []

    for(int i = 0; i < dockerImages.size(); i++) {
        def imgDetails = dockerImages[i]
        
        // TODO: Need to standardize this. Maybe the caller should do this?? Not sure.
        def imageNameSuffix = arch && arch == 'arm64' ? "-${arch}" : ''
        def imageName = "${imgDetails.image}${imageNameSuffix}"

        def taggedImages = push(imageName, latest, nexusRepo)

        // grab the first tag for Clair scan later
        if(taggedImages) {
            pushedImages << taggedImages.first()
        }
    }

    pushedImages
}

def getDockerTags(latest = true, customTags = env.DOCKER_CUSTOM_TAGS) {
    def allTags = []

    if(env.GIT_COMMIT) {
        allTags << "${env.GIT_COMMIT}"
    }

    if(latest) {
        allTags << 'latest'
    }

    if(env.VERSION) {
        allTags << env.VERSION
        allTags << "${env.GIT_COMMIT}-${env.VERSION}"
    }

    if(env.SEMVER_BRANCH) {
        allTags << env.SEMVER_BRANCH
    }

    if(env.BUILD_EXPERIMENTAL_DOCKER_IMAGE == 'true') {
        allTags << 'experimental'
    }

    if(env.BUILD_STABLE_DOCKER_IMAGE == 'true') {
        allTags << 'stable'
    }

    if(customTags) {
        customTags.split(' ').each {
            allTags << it
        }
    }

    allTags
}

def finalImageName(imageName) {
    def finalDockerImageName = imageName

    // prepend with registry "namespace" if not empty
    if(env.DOCKER_REGISTRY_NAMESPACE && env.DOCKER_REGISTRY_NAMESPACE != '/') {
        finalDockerImageName = "${env.DOCKER_REGISTRY_NAMESPACE}/${finalDockerImageName}"
    }

    finalDockerImageName
}

def cleanImageUrl(imageUrl) {
    imageUrl.replaceAll(/^https?:\/\//, '')
}

def parse(imageUrl, useLatest = true, defaultRegistry='docker.io') {
    def parsedImage
    //println "[edgeXDocker.parse] Parsing ${imageUrl}"
    try {
        def s = cleanImageUrl(imageUrl)

        // grab the first /
        def splitIndex = s.indexOf('/')

        // no host maybe just an official image
        if(splitIndex == -1) {
            def host = defaultRegistry
            def fullImage = s
            def fullImageSplit = s.split(':')
            def image = fullImageSplit[0]
            def tag = fullImageSplit.size() > 1 ? fullImageSplit[1] : null
            if(!tag && useLatest) {
                tag = 'latest'
            }
            parsedImage = [ host: host, fullImage: fullImage, namespace: null, image: image, tag: tag]
        } else {
            def host = s.substring(0, splitIndex)
            def namespace = null
            def fullImage = s.substring(splitIndex+1)

            // handle the use case where no hostname is passed in
            // default values to the defaultRegistry
            if(!host.contains('.')) {
                namespace = host
                fullImage = "${namespace}/${fullImage}"
                host = defaultRegistry
            }

            def fullImageSplit = fullImage.split(':')
            def image = fullImageSplit[0]

            if(image.contains('/')) {
                namespace = image.split('/').first()
                image = image.split('/').last()
            }

            def tag = fullImageSplit.size() > 1 ? fullImageSplit[1] : null

            if(!tag && useLatest) {
                tag = 'latest'
            }

            parsedImage = [ host: host, fullImage: fullImage, namespace: namespace, image: image, tag: tag]
        }
    } catch (e) {
        println "[edgeXDocker.parse] Unable to parse image url...Error: ${e.message}"
        parsedImage = null
    }

    parsedImage
}

def toImageStr(image, defaultRegistry='docker.io') {
    def imgBuffer = [
        image.host ? image.host : defaultRegistry,
        '/',
        image.namespace ? "${image.namespace}/" : null,
        image.image,
        image.tag ? ":${image.tag}" : null
    ]

    (imgBuffer - null).join('')
}