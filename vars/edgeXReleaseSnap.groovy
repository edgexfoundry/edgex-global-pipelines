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

/*

releaseYaml:

---
name: 'sample-service'
version: 1.1.2
releaseStream: 'master'
repo: 'https://github.com/edgexfoundry/sample-service.git'
snapChannels:
  - channel: 'latest/stable'
    revisionNumber: 2218
  - channel: 'geneva/stable'
    revisionNumber: ~
snap: true

edgeXReleaseSnap(releaseYaml)

*/

def call(releaseInfo) {
    if(releaseInfo.containsKey("snap")) {
        if(releaseInfo["snap"]) {
            validate(releaseInfo)
            releaseSnap(releaseInfo)
        }
        else {
            echo("[edgeXReleaseSnap]: repo is not snap enabled")
        }
    }
}

def validate(releaseInfo) {
    // raise error if releaseInfo map does not contain required attributes
    if(!releaseInfo.name) {
        error("[edgeXReleaseSnap]: Release yaml does not contain 'name'")
    }
    if(!releaseInfo.version) {
        error("[edgeXReleaseSnap]: Release yaml does not contain 'version'")
    }
    if(!releaseInfo.snapChannels) {
        error("[edgeXReleaseSnap]: Release yaml does not contain 'snapChannels'")
    }
    if(!releaseInfo.releaseStream) {
        error("[edgeXReleaseSnap]: Release yaml does not contain 'releaseStream'")
    }
    if(!releaseInfo.repo) {
        error("[edgeXReleaseSnap]: Release yaml does not contain 'repo'")
    }
}

def getSnapcraftAddress(repo, branch) {
    // return the raw content address for snapcraft yaml
    repo.replaceAll("\\.git", "").replaceAll("https://github.com/", "https://raw.githubusercontent.com/") + "/${branch}/snap/snapcraft.yaml"
}

def getSnapMetadata(repo, branch, name) {
    // return snap meta data for repo
    println "[edgeXReleaseSnap]: getting snap metadata for repo: ${repo} branch: ${branch}"
    def snapCraftAddress = getSnapcraftAddress(repo, branch)
    sh "curl --fail -o ${env.WORKSPACE}/snapcraft-${name}.yaml -O ${snapCraftAddress}"
    readYaml(file: "${env.WORKSPACE}/snapcraft-${name}.yaml")
}

def getSnapInfo(snapName) {
    // return info from snapcraft.io store for snapName
    println "[edgeXReleaseSnap]: getting info from snapcraft for snapName: ${snapName}"
    def snapInfoStdout = sh(
        script: "curl --fail -H 'Snap-Device-Series: 16' 'https://api.snapcraft.io/v2/snaps/info/${snapName}?fields=name,revision'",
        returnStdout: true).trim()
    readJSON(text: snapInfoStdout)
}

def getSnapRevision(snapInfo, architecture, trackName) {
    // return revision within snapInfo that matches architecture and trackName
    println "[edgeXReleaseSnap]: getting revison for trackName: ${trackName} architecture: ${architecture}"
    def trackNameSplit = trackName.split('/')
    def track = trackNameSplit[0]
    def name = trackNameSplit[1]
    def found = snapInfo.'channel-map'.find { item ->
        (item.channel.architecture == architecture && item.channel.track == track && item.channel.name == name) 
    }
    if(found) {
        return found.revision
    }
    else {
        return '1'
    }
}

def releaseSnap(releaseInfo) {
    // exception handled function that releases snap
    try {
        println "[edgeXReleaseSnap]: releasing snaps for: ${releaseInfo.name}"
        // get snap metadata from snapcraft.yaml located in the repo
        def snapMetadata = getSnapMetadata(releaseInfo.repo, releaseInfo.releaseStream, releaseInfo.name)
        // get snap info from snapcraft.io store
        def snapInfo = getSnapInfo(snapMetadata.name)
        snapMetadata.architectures.each { architecture ->
            if(['amd64', 'arm64'].contains(architecture.'build-on')) {
                // edgeXSnap requires architecture to be set as environment variable
                releaseInfo.snapChannels.each { snapChannel ->
                    def snapRevision
                    if(snapChannel.revisionNumber) {
                        snapRevision = snapChannel.revisionNumber
                    }
                    else {
                        // attempt to find revision that matches architecture for trackName
                        // if no trackName specified default to 'latest/edge'
                        def trackName = snapChannel.trackName ?: 'latest/edge'
                        snapRevision = getSnapRevision(snapInfo, architecture.'build-on', trackName)
                    }
                    withEnv(["ARCH=${architecture.'build-on'}"]) {
                        def message = """[edgeXReleaseSnap]:\
                            edgeXSnap(jobType: 'release',\
                                snapChannel: ${snapChannel.channel},\
                                snapRevision: ${snapRevision},\
                                snapName: ${snapMetadata.name}) - DRY_RUN: ${env.DRY_RUN}"""
                        echo(message.replaceAll("\\s{2,}", " ").trim())
                        if(!edgex.isDryRun()) {
                            edgeXSnap([
                                jobType: 'release',
                                snapChannel: snapChannel.channel,
                                snapRevision: snapRevision,
                                snapName: snapMetadata.name])
                        }
                    }
                }
            }
            else {
                echo("[edgeXReleaseSnap]: architecture ${architecture.'build-on'} is not supported")
            }
        }
    }
    catch(Exception ex) {
        error("[edgeXReleaseSnap]: ERROR occurred releasing snap: ${ex}")
    }
}
