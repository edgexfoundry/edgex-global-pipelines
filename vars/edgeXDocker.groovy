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

    if(env.http_proxy) {
        buildArgs << "http_proxy"
        buildArgs << "https_proxy"
    }

    if(env.DOCKER_BUILD_ARGS) {
        env.DOCKER_BUILD_ARGS.split(',').each { buildArgs << it }
    }

    def buildArgString = buildArgs.join(' --build-arg ')

    def labels = ['', "'git_sha=${GIT_COMMIT}'", "'arch=${ARCH}'"]

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

def push(dockerImageName, latest = true) {
    def allTags = [env.GIT_COMMIT]

    if(latest) {
        allTags << 'latest'
    }

    if(env.VERSION) {
        allTags << env.VERSION
        allTags << "${GIT_COMMIT}-${VERSION}"
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

    docker.withRegistry("https://${DOCKER_REGISTRY}") {
        allTags.each {
            image.push(it)
        }
    }
}

def finalImageName(imageName) {
    def finalDockerImageName = imageName

    // prepend with registry "namespace" if not empty
    if(env.DOCKER_REGISTRY_NAMESPACE && env.DOCKER_REGISTRY_NAMESPACE != '/') {
        finalDockerImageName = "${DOCKER_REGISTRY_NAMESPACE}/${finalDockerImageName}"
    }

    finalDockerImageName
}