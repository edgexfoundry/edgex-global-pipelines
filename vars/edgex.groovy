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

def isReleaseStream(branchName = env.GIT_BRANCH) {
    // what defines a main release branch
    def releaseStreams = [/^master$/, /^california$/, /^delhi$/, /^edinburgh$/, /^fuji$/]
    env.SILO == 'production' && (branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true))
}

def didChange(expression, previous='origin/master') {
    // If there was no previous successful build (as in building for first time) will return true.
    def diffCount = 0

    println "[didChange-DEBUG] checking to see what changed in this build...looking for expression: [${expression}]"

    // if no previous commit, then this is probably the first build
    if (previous == null) {
        println "[didChange-DEBUG] NO previous commit. Probably the first build"
        diffCount = 1
    } else{
        // If we are merging into master then both previous and the current will be the same
        // so we need to calculate the previous commit has
        if(previous =~ /.*master|.*release/ && env.GIT_BRANCH =~ /.*master|.*release/) {
            println "[didChange-DEBUG] currently merging into ${env.GIT_BRANCH}, need to lookup previous commit sha1"
            previous = sh (script: "git show --pretty=%H ${env.GIT_COMMIT}^1 | xargs", returnStdout: true).trim()
        }

        println "[didChange-DEBUG] we have a previous commit: [${previous}]"
        println "[didChange-DEBUG] Files changed since the previous commit:"

        sh "git diff --name-only ${env.GIT_COMMIT} ${previous} | grep \"${expression}\" | true"

        diffCount = sh (
          returnStdout: true,
          script: "git diff --name-only ${env.GIT_COMMIT} ${previous} | grep \"${expression}\" | wc -l"
        ).trim().toInteger()
    }

    return diffCount > 0
}

def mainNode(config) {
    def defaultNode = config.nodes.find { it.isDefault == true }
    defaultNode ? defaultNode.label : 'centos7-docker-4c-2g'
}

def nodeExists(config, arch) {
    config.nodes.collect { it.arch == arch }.contains(true)
}

def getNode(config, arch) {
    def node = config.nodes.find { it.arch == arch }
    node ? node.label : mainNode(config)
}

def setupNodes(config) {
    def defaultNodes = [
        [label: 'centos7-docker-4c-2g', arch: 'amd64', isDefault: true],
        [label: 'ubuntu18.04-docker-arm64-4c-16g', arch: 'arm64', isDefault: false]
    ]

    def _arch = config.arch ?: ['amd64', 'arm64']

    println "Setting up nodes based on requested architectures [${_arch}]"

    def _nodes = []

    _arch.each { architecture ->
        def node = defaultNodes.find { it.arch == architecture }
        if(node) {
            _nodes << node
        }
    }

    // in case no nodes are found, just use out defaults
    if(!_nodes) {
        _nodes = defaultNodes
    }

    println "Nodes requested: [${_nodes.collect { it.label }}]"
    config.nodes = _nodes
}

def getVmArch() {
    def vmArch = sh(script: 'uname -m', returnStdout: true).trim()
    if(vmArch == 'aarch64') {
        vmArch = 'arm64'
    }
    vmArch
}

def bannerMessage(msg) {
    echo """=========================================================
 ${msg}
========================================================="""
}

def printMap(map) {
    def longestKey
    def longest = 0

    map.each { k,v ->
        if(k.length() > longest) {
            longest = k.length()
            longestKey = k
        }
    }

    def msg = []
    map.each { k,v ->
        msg << "${k.padLeft(longestKey.length())}: ${v}"
    }

    echo msg.join("\n")
}

def defaultTrue(value) {
    (value == true || value == null) ? true : false
}

def defaultFalse(value) {
    (value == false || value == null) ? false : true
}

def releaseInfo(tagVersions='stable experimental') {
    def credId = (env.SILO == 'production') ? 'edgex-jenkins-github-personal-access-token' : 'edgex-jenkins-access-username'
    // these env var names are declararive syntax norms
    withCredentials([usernamePassword(credentialsId: credId, usernameVariable: 'GH_TOKEN_USR', passwordVariable: 'GH_TOKEN_PSW')]) {
        withEnv(["EGP_TAG_VERSIONS=${tagVersions}"]) {
            bannerMessage "EdgeX Global Pipelines Version Info"
            sh(script: libraryResource('releaseinfo.sh'))
        }
    }
}

def isDryRun() {
    // return True if DRY_RUN is set False otherwise
    [null, '1', 'true'].contains(env.DRY_RUN)
}

def isMergeCommit(commit) {
    def s = "git rev-list -1 --merges ${commit}~1..${commit}"
    def mergeCommitParent = sh(script: s, returnStdout: true)
    println "-----------> ${s} ${mergeCommitParent} ${commit} [${mergeCommitParent == commit}]"
    (mergeCommitParent == commit)
}

// This method handles the use case for
// merge-commit vs squash-commit Github merge types
def getPreviousCommit(commit) {
    def previousCommit
    def mergeCommit = isMergeCommit(commit)

    if(mergeCommit) {
        // the merge commit previous commit is pulled by
        // looking the at commit parents
        previousCommit = sh(script: "git rev-list --parents -n 1 ${commit} | cut -d' ' -f3", returnStdout: true)
    } else {
        // easier lookup HEAD~1
        previousCommit = sh(script: 'git show --pretty=%H HEAD~1 | xargs', returnStdout: true)
    }
    previousCommit
}

def getBranchName() {
    sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
}

// Get the commit message from the commit sha
def getCommitMessage(commit) {
    sh(script: "git log --format=format:%s -1 ${commit}", returnStdout: true).trim()
}

// Return true when the commit message follows the pattern "build(...): [semanticVersion,namedTag] ... "
def isBuildCommit(commit) {    
    return !!(commit =~ /^build\(.+\): \[(?<semver>(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)\,(?<namedTag>\w+)\].+$/)
}

// Return the paramters for the build [semanticVersion,namedTag]
def parseBuildCommit(commit) {
    try {
        def matcher =  (commit =~ /^build\(.+\): \[(?<semver>(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)\,(?<namedTag>\w+)\].+$/)
        matcher.matches()
        return [version: matcher.group('semver'), namedTag: matcher.group('namedTag')]
    } catch (Exception e) {
        error("[edgex.parseBuildCommit]: No matches found.")
    }
}

def getTmpDir(pattern = 'ci-XXXXX') {
    sh(script: "mktemp -d -t ${pattern}", returnStdout: true).trim()
}
