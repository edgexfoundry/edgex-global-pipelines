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

def build(dockerImageName, baseImage = null) {
    def buildArgs = ['']

    if(baseImage) {
        buildArgs << "BASE='${baseImage}'"
    }

    if(env.BUILD_SCRIPT) {
        buildArgs << "MAKE='${BUILD_SCRIPT}'"
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

    def labels = ['', "'git_sha=${GIT_COMMIT}'", "'arch=${buildArgArch}'"]

    // in case VERSION is not set (i.e. use semver = fallse)
    if(env.VERSION) {
        labels << "'version=${VERSION}'"
    }

    // devops labels go here (date, branch, build number, etc)
    def labelString = labels.join(' --label ')

    // for LF jenkins, reset owner before build
    sh 'sudo chown -R jenkins:jenkins .'
    sh 'ls -al .'

    docker.build(finalImageName(dockerImageName), "-f ${DOCKER_FILE_PATH} ${buildArgString} ${labelString} ${DOCKER_BUILD_CONTEXT}")
}

def push(dockerImageName, latest = true, nexusRepo = 'staging') {
    def taggedImages = []

    def nexusPortMapping = [
        snapshots: 10003,
        snapshot: 10003,
        staging: 10004,
        release: 10002
    ]

    def nexusPort = nexusPortMapping[nexusRepo]

    def allTags = ["${env.GIT_COMMIT}"]

    if(latest) {
        allTags << 'latest'
    }

    if(env.VERSION) {
        allTags << env.VERSION
        allTags << "${GIT_COMMIT}-${VERSION}"
    }

    if(env.SEMVER_BRANCH) {
        allTags << env.SEMVER_BRANCH
    }

    def customTags = env.DOCKER_CUSTOM_TAGS
    if(customTags) {
        customTags.split(' ').each {
            allTags << it
        }
    }

    def finalDockerImageName = finalImageName(dockerImageName)

    def image = docker.image(finalDockerImageName)

    println """[edgeXDocker.push] Tagging docker image ${finalDockerImageName} with the following tags:
${allTags.join('\n')}
====================================================="""

    docker.withRegistry("https://${DOCKER_REGISTRY}:${nexusPort}") {
        allTags.each {
            image.push(it)
            taggedImages << "${DOCKER_REGISTRY}:${nexusPort}/${finalDockerImageName}:${it}"
        }
    }

    taggedImages
}

def finalImageName(imageName) {
    def finalDockerImageName = imageName

    // prepend with registry "namespace" if not empty
    if(env.DOCKER_REGISTRY_NAMESPACE && env.DOCKER_REGISTRY_NAMESPACE != '/') {
        finalDockerImageName = "${DOCKER_REGISTRY_NAMESPACE}/${finalDockerImageName}"
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