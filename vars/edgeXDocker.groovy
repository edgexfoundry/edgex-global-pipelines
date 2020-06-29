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

def buildInParallel(dockerImages, baseImage = null) {
    //check if docker-compose --parallel is available
    def composeImage = env.ARCH == 'arm64'
        ? 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose-arm64:latest'
        : 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest'

    def parallelSupported = -1

    docker.image(composeImage).inside {
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
        def dockerCompose = generateDockerComposeForBuild(dockerImages, labels, env.ARCH)

        // write ephemeral docker-compose file
        writeFile(file: './docker-compose-build.yml', text: dockerCompose)

        // always set builder base in case it is not setup in the user's Jenkinsfile
        def envVars = baseImage ? ["BUILDER_BASE=${baseImage}"] : []

        withEnv(envVars) {
            docker.image(composeImage).inside('-u 0:0 -v /var/run/docker.sock:/var/run/docker.sock --privileged') {
                sh 'docker-compose -f ./docker-compose-build.yml build --parallel'
            }

            sh 'docker images | grep docker' //debug
        }
    } else {
        error '[edgeXDocker] --parallel build is not supported in this version of docker-compose'
    }
}

def generateDockerComposeForBuild(services, labels, arch = null) {
"""
version: '3.7'
services:
${services.collect { generateServiceYaml(it.image, 'docker-', it.dockerfile, labels, arch) }.join('\n') }
"""
}

def generateServiceYaml(serviceName, imageNamePrefix, dockerFile, labels, arch = null) {
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

    def image = docker.image(finalImage.image)

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

    if(customTags) {
        customTags.split(' ').each {
            allTags << it
        }
    }

    allTags
}

/* Promote list of images
    pseudo code
    when repo = snapshot
        tag with git sha
        push to snapshots repo to promotion namespace (promotion namespace allows a common place for all images needing promotion to land)
    when repo = staging
        get previous commit sha
        pull last image promoted to snapshots with previous commit sha
        if a version is provided, relabel docker image with new version and new git commit sha
        else only relabel with new git commit sha
        push relabeled image to staging with default tags

    def images = [
        'edgexfoundry/docker-core-metadata-go',
        'edgexfoundry/docker-core-metadata-go-arm64'
    ]
    usage: promote(images, 'staging', env.GIT_COMMIT)
*/
def promote(dockerImages, nexusRepo, commit, version) {
    def promotedImages = []
    dockerImages.each { imageUrl ->
        def parsedImage = parse(imageUrl)
        def taggedImages

        if(nexusRepo == 'staging') {
            def promoteFrom = env.DOCKER_REGISTRY ? "${env.DOCKER_REGISTRY}:10004" : 'nexus3.edgexfoundry.org:10004'
            def previousCommit = edgex.getPreviousCommit(commit)
            def imageToPromote = "${promoteFrom}/${parsedImage.image}:${previousCommit}"
            def stagingImage = "${parsedImage.image}:promoting"

            sh "docker pull ${imageToPromote}"

            // relabel and retag
            def labels = ['git_sha': commit]
            if(version) {
                labels << ["version": version]
                relabel(imageToPromote, stagingImage, labels, false)
            } else {
                relabel(imageToPromote, stagingImage, labels, false)
            }

            println "[edgexDocker] pushing ${stagingImage} to [${nexusRepo}]"

            def tags = getDockerTags(true)

            taggedImages = push(stagingImage, false, nexusRepo, tags)
        } else if(nexusRepo == 'snapshot') {
            // in this use case the image should already
            // have been built on the host, so no need to pull
            def promotionNamespace = env.DOCKER_PROMOTION_NAMESPACE ?: 'edgex-promotion'

            // when deploying to sandbox only use the commit tag
            def tags = [ commit ]

            withEnv(["DOCKER_REGISTRY_NAMESPACE=${promotionNamespace}"]) {
                taggedImages = push(parsedImage.image, false, nexusRepo, tags)
            }
        }

        if(taggedImages) {
            promotedImages.addAll(taggedImages)
        }
    }

    promotedImages
}

/*
    # https://github.com/moby/moby/issues/3465#issuecomment-383416201
    pseudo code
        pull image if needed
        save docker image as tar
        untar docker image layers
        read manifest.json and extract docker image config json
        read docker image config json
        for each label to change
            Update config JSON where labels are stored
            - config.Labels
            - container_config.Labels
            - container_config.Cmd
            - history
        tar files again
        docker load tar
        retag docker image
*/
def relabel(dockerImage, newDockerImage, labels, pull = true) {
    // if no new image is specified, retag same image
    def relabeledJson

    if(pull) {
        sh "docker pull ${dockerImage}"
    }

    //Do this work in a tmp dir to avoid dirtying the workspace
    def tmpDir = edgex.getTmpDir()
    dir(tmpDir) {
        sh "docker save -o ./image.tar ${dockerImage}"
        sh 'tar -xvf image.tar && rm -rf image.tar'

        def config = getDockerConfigJson('./manifest.json')

        if(config.json) {
            labels.each { key, value ->
                //first place to check: config.Labels
                def configLabel = config.json.config.Labels[key]

                if (configLabel) {
                    // println "Found the following for ${key} [${configLabel}]"
                    // println "Writing new value: [${value}]"
                    config.json.config.Labels[key] = value
                    // println "New value [${config.json.config.Labels[key]}]"
                }

                //container_config.Labels
                def containerConfigLabel = config.json.container_config.Labels[key]

                if (containerConfigLabel) {
                    // println "Found the following for ${key} [${containerConfigLabel}]"
                    // println "Writing new value: [${value}]"
                    config.json.container_config.Labels[key] = value
                    // println "New value [${config.json.container_config.Labels[key]}]"
                }

                // rebuild container_config.Cmd
                if (config.json.container_config.Cmd) {
                    def newCmd = []
                    config.json.container_config.Cmd.each { cmdSpec ->
                        if (cmdSpec =~ /LABEL $key/) {
                            def newLabelStr = replaceDockerLabel(cmdSpec, key, value)
                            newCmd << newLabelStr
                        } else {
                            newCmd << cmdSpec
                        }
                    }
                    config.json.container_config.Cmd = newCmd
                }

                // rebuild history
                config.json.history.each { historySpec ->
                    if (historySpec.created_by =~ /LABEL $key/) {
                        def newLabelStr = replaceDockerLabel(historySpec.created_by, key, value)
                        historySpec.created_by = newLabelStr
                    }
                }

                // println config.json //debug
            }

            // new json should be good here. lets write it out
            // this will replace existing file
            writeJSON file: config.filename, json: config.json

            relabeledJson = config.json // this was added to facilitate testing

            sh 'tar -cvf image.tar *'
            sh 'docker load -i image.tar'

            if (newDockerImage) {
                def tagCommand = [
                        'docker',
                        'tag',
                        dockerImage.replaceAll('\n', ''),
                        newDockerImage.replaceAll('\n', '')
                ]
                sh tagCommand.join(' ')
            }
        }
    }

    relabeledJson
}

def getDockerConfigJson(manifestFile) {
    def manifest = readJSON file: manifestFile
    def configJSON = readJSON file: "./${manifest.Config[0]}"

    [ filename: manifest.Config[0], json: configJSON ]
}

def replaceDockerLabel(labelStr, key, value) {
    // println "Old Label: ${labelStr}" // debug
    def newLabelStr = labelStr.replaceAll(/LABEL $key=(.*)/, "LABEL ${key}=${value}")
    // println "New Label: ${newLabelStr}" // debug

    newLabelStr
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