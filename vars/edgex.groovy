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
 # edgex

 ## Overview

 Shared library of common helper functions for all EdgeX Jenkins pipelines.

 ## Functions

 - `edgex.isReleaseStream`: Used to validate whether the current branch that Jenkins is building is a branch that is considered a branch with "releasable" artifacts (i.e docker push, git semver push).
 - `edgex.isLTS`: Used to determine if the current branch that Jenkins is building is for an LTS release.
 - `edgex.getTargetBranch`: Used to determine the target branch that is being merged into for a PR.
 - `edgex.didChange`: Determine if the given expression matches the files that changed in a given PR or merge. For example:
    ```groovy
    // Did any .go files change from the current branch compared to origin/main
    didChange('*.go')

    // Did any .yaml files change from the current branch compared to origin/release
    didChange('*.yaml', 'origin/release')
    ```
 - `edgex.mainNode`: Given a config map with `config.nodes`, either return the node label marked as `defaultNode = true` or return the DevOps managed "default" node label.
 - `edgex.nodeExists`: Verify a given node architecture matches provided architecture.
 - `edgex.getNode`: Return node with architecture that matches provided architecture.
 - `edgex.setupNodes`: Setup default node labels for x86_64 and arm64 nodes.
 - `edgex.getVmArch`: Run uname to determine VM architecture. If `aarch64` is returned, convert to the result to `arm64`.
 - `edgex.bannerMessage`: Vanity function to wrap given input message with a banner style output for easier readability in Jenkins console.
 - `edgex.printMap`: Vanity function to print Groovy Map to the Jenkins console.
 - `edgex.defaultTrue`: Returns true if the input is `true` or `null`. This is useful to setup default values in functions when none is provided.
 - `edgex.defaultFalse`: Returns true if the input is `false` or `null`. This is useful to setup default values in functions when none is provided.
 - `edgex.releaseInfo`: Call shell script `resources/releaseinfo.sh` to output current edgex-global-pipeline version information in the Jenkins console. This is really useful for debugging older builds in case issues are discovered.
 - `edgex.isDryRun`: Whether or not the `env.DRY_RUN` environment variable is set. Will return `true` if DRY_RUN is set, `false` otherwise.
 - `edgex.isMergeCommit`: Determines if the current commit Jenkins is building is considered a git "merge commit". Useful if determining parent commit info.
 - `edgex.getPreviousCommit`: Determines the previous commit SHA given the merge commit or squash commit git use-cases. Different git commands have to be run to be able to determine the previous commit.
 - `edgex.getBranchName`: Returns the current branch name from git.
 - `edgex.getCommitMessage`: Returns the current commit message from git given a commit SHA.
 - `edgex.isBuildCommit`: Return true when the commit message follows the pattern `build(...): [semanticVersion,namedTag] ... `.
 - `edgex.parseBuildCommit`: Return the parameters for the build `[semanticVersion,namedTag]`.
 - `edgex.getTmpDir`: Run `mktemp` with given pattern to create a temporary directory in `/tmp`.
 - `edgex.getGoLangBaseImage`: Return DevOps managed base images used in Go pipelines.
 - `edgex.isGoProject`: Looks at repository directory structure to determine if the repository is Golang based. Uses the existence of the `go.mod` file.
 - `edgex.getCBaseImage`: Return the base image used as the base image for all C based repositories.
 - `edgex.parallelJobCost`: Wraps call to `lfParallelCostCapture` inside docker image to save time downloading pip dependencies.
 - `edgex.patchAlpineSeccompArm64`: A fix for arm64 nodes that enables a security profile for docker. Another workaround is to just use the `--privileged` docker flag.
 - `edgex.isLTSReleaseBuild`: Returns `true` if current commit message begins with `ci(lts-release)`.
 - `edgex.semverPrep`: Poorly named function that sets up the `env.NAMED_TAG` and `env.BUILD_STABLE_DOCKER_IMAGE` for the build commit concept. Will be removed in a future release.
 - `edgex.waitFor`: Useful function to wait for a condition in a shell script to be met.
 - `edgex.waitForImages`: Useful function to determine if a docker image has been pushed to a repository.
 - `edgex.commitChange`: Commits a change to the repo with a given message.
 - `edgex.createPR`: Creates a PR with the [GitHub CLI](https://cli.github.com/) for with a given branch, title, message and reviewers for. Note: This is generic enough to be used in other functions.
*/

def isReleaseStream(branchName = env.GIT_BRANCH) {
    // what defines a main release branch
    def releaseStreams = [/^main$/, /^master$/, /^california$/, /^delhi$/, /^edinburgh$/, /^fuji$/, /^geneva$/, /^hanoi$/, /^ireland$/, /^lts-test$/, /^jakarta$/, /^kamakura$/, /^levski$/, /^minnesota$/]
    env.SILO == 'production' && (branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true))
}

def isLTS() {
    def branchName = getTargetBranch()
    def ltsStreams = [/^jakarta$/, /^lts-test$/]
    println "[edgeX.isLTS] Checking if [${branchName}] matches against LTS streams [${ltsStreams}]"
    (branchName && (ltsStreams.collect { branchName =~ it ? true : false }).contains(true))
}

def getTargetBranch() {
    // if CHANGE_ID is present, then we are dealing with a PR, we need to check against
    // the CHANGE_TARGET rather than GIT_BRANCH
    (env.CHANGE_ID && env.CHANGE_TARGET) ? env.CHANGE_TARGET : env.GIT_BRANCH
}

def didChange(expression, previous='origin/main') {
    // If there was no previous successful build (as in building for first time) will return true.
    def diffCount = 0

    println "[didChange-DEBUG] checking to see what changed in this build...looking for expression: [${expression}]"

    // if no previous commit, then this is probably the first build
    if (previous == null) {
        println "[didChange-DEBUG] NO previous commit. Probably the first build"
        diffCount = 1
    } else{
        // If we are merging into main then both previous and the current will be the same
        // so we need to calculate the previous commit has
        if(previous =~ /.*main|.*release|.*release-lts/ && env.GIT_BRANCH =~ /.*main|.*release|.*release-lts/) {
            println "[didChange-DEBUG] currently merging into ${env.GIT_BRANCH}, need to lookup previous commit sha1"
            previous = sh (script: "git show --pretty=%H ${env.GIT_COMMIT}^1 | head -n 1 | xargs", returnStdout: true).trim()
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
    defaultNode ? defaultNode.label : 'ubuntu20.04-docker-8c-8g'
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
        [label: 'ubuntu20.04-docker-8c-8g', arch: 'amd64', isDefault: true],
        [label: 'ubuntu20.04-docker-arm64-4c-16g', arch: 'arm64', isDefault: false]
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
    [null, '1', 'true'].contains(env.DRY_RUN)
}

def isMergeCommit(commit) {
    def s = "git rev-list -1 --merges ${commit}~1..${commit}"
    def mergeCommitParent = sh(script: s, returnStdout: true)
    println "-----------> ${s} ${mergeCommitParent} ${commit} [${mergeCommitParent == commit}]"
    (mergeCommitParent == commit)
}

def getPreviousCommit(commit) {
    def previousCommit
    def mergeCommit = isMergeCommit(commit)

    if(mergeCommit) {
        // the merge commit previous commit is pulled by
        // looking the at commit parents
        previousCommit = sh(script: "git rev-list --parents -n 1 ${commit} | cut -d' ' -f3", returnStdout: true)
    } else {
        // easier lookup HEAD~1
        previousCommit = sh(script: 'git show --pretty=%H HEAD~1 | head -n 1 | xargs', returnStdout: true)
    }
    previousCommit
}

def getBranchName() {
    sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
}

def getCommitMessage(commit) {
    sh(script: "git log --format=format:%s -1 ${commit}", returnStdout: true).trim()
}

// TODO: remove build commit functions
def isBuildCommit(commit) {
    return !!(commit =~ /^build\(.+\): \[(?<semver>(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)\,(?<namedTag>\w+)\].+$/)
}

// TODO: remove build commit functions
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

def getGoLangBaseImage(version, alpineBased) {
    def baseImage

    if(alpineBased == true || alpineBased == 'true') {
        def goBaseImages = [
            '1.11': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.11.13-alpine',
            '1.12': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine',
            '1.13': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
            '1.15': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.15-alpine',
            '1.16': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.16-alpine',
            '1.17': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.17-alpine',
            '1.18': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.18-alpine',
            '1.20': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.20-alpine'
        ]

        def goLTSImages = [
            '1.16': 'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.16-alpine-lts',
            '1.17': 'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.17-alpine-lts'
        ]

        // isLTS uses env.GIT_BRANCH to determine if the build is a LTS build
        if(isLTS()) {
            baseImage = goLTSImages[version]
        } else {
            baseImage = goBaseImages[version]
        }

        if(!baseImage) {
            baseImage = "golang:${version}-alpine"
        }
    } else {
        baseImage = "golang:${version}"
    }

    baseImage
}

def getGoModVersion() {
    sh(script: "grep '^go [0-9].[0-9]*' go.mod | cut -d' ' -f 2", returnStdout: true).trim()
}

def isGoProject(folder) {
    if(folder) {
        dir(folder) {
            fileExists('go.mod')
        }
    } else  {
        fileExists('go.mod')
    }
}

def getCBaseImage(version = 'latest') {
    isLTS()
    ? 'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-gcc-base:gcc-lts'
    : "nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:${version}"
}

def parallelJobCost(tag='latest') {
    docker.image("nexus3.edgexfoundry.org:10003/edgex-lftools-log-publisher:${tag}")
        .inside('-u 0:0 --privileged --net host -v /home/jenkins:/home/jenkins -v /run/cloud-init/result.json:/run/cloud-init/result.json')
    {
        lfParallelCostCapture()
    }
}

// Temporary fix for this issue. Will look to fix this in packer image after Jakarta
// Issue with alpine:3.14 and older docker versions
// See: https://wiki.alpinelinux.org/wiki/Release_Notes_for_Alpine_3.14.0#faccessat2
def patchAlpineSeccompArm64() {
    sh 'sudo curl -o /etc/docker/seccomp.json "https://raw.githubusercontent.com/moby/moby/master/profiles/seccomp/default.json"'
    sh 'sudo sed -i \'s/"defaultAction": "SCMP_ACT_ERRNO"/"defaultAction": "SCMP_ACT_TRACE"/g\' /etc/docker/seccomp.json'
    sh 'sudo jq \'. += {"seccomp-profile": "/etc/docker/seccomp.json"}\' /etc/docker/daemon.json | sudo tee /etc/docker/daemon.new'
    sh 'sudo mv /etc/docker/daemon.new /etc/docker/daemon.json'
    sh 'sudo service docker restart'
}

// TODO: Refactor to edgeXLTS
def isLTSReleaseBuild(commit = env.GIT_COMMIT) {
    def noopMessages = [/^ci\(lts-release\)/]

    // Merge commits should not happen for a lts-release, but I am just being paranoid here
    // need to resolve the correct commit sha. If we are on a merge commit, then we need to lookup commit sha from the previous commit
    // TODO: getPreviousCommit() already calls isMergeCommit. Remove the use of isMergeCommit here. Needs testing before this change is merged.
    def resolvedCommit = isMergeCommit(commit) ? getPreviousCommit(commit) : commit

    def commitMsg = getCommitMessage(resolvedCommit)
    def isLTSRelease = (commitMsg && (noopMessages.collect { commitMsg =~ it ? true : false }).contains(true))

    // This handles the case when the build is triggered with a CommitId, typically in
    // the release process, we the commit message does not change, but we still need
    // to do the regular build process
    if(isLTSRelease && env.CommitId) {
        isLTSRelease = false
    }

    if(isLTSRelease) {
        bannerMessage "[isLTSReleaseBuild] No build required. isLTSRelease: [${isLTSRelease}]"
    }
    else {
        bannerMessage "[isLTSReleaseBuild] Regular build required. isLTSRelease: [${isLTSRelease}] ${(env.CommitId) ? 'However, we have a env.CommitId' : ''}"
    }
    isLTSRelease
}

// TODO: remove build commit functions
def semverPrep(commit = env.GIT_COMMIT) {
    def commitMsg = getCommitMessage(commit)
    println "[semverPrep] GIT_COMMIT: ${commit}, Commit Message: ${commitMsg}"

    def buildVersion = null
    if(isBuildCommit(commitMsg)) {
        def parsedCommitMsg = parseBuildCommit(commitMsg)
        buildVersion = parsedCommitMsg.version

        println "[semverPrep] This is a build commit. buildVersion: [${buildVersion}], namedTag: [${parsedCommitMsg.namedTag}]"

        env.NAMED_TAG = parsedCommitMsg.namedTag
        env.BUILD_STABLE_DOCKER_IMAGE = true
    }
    else {
        println "[semverPrep] This is not a build commit."
        env.BUILD_STABLE_DOCKER_IMAGE = false
    }

    buildVersion
}

def waitFor(command, timeoutMinutes = 60, exitCode = 0, sleepFor = 5) {
    // the writeFile allows us to call the script with arguments
    def waitScript = libraryResource('wait-for.sh')
    writeFile(file: './wait-for.sh', text: waitScript)
    sh 'chmod +x ./wait-for.sh'

    try {
        timeout(timeoutMinutes) {
            sh "./wait-for.sh '${command}' ${exitCode} ${sleepFor}"
        }
    } catch (e) {
        error("Timeout reached for command [${command}]")
    }
}

def waitForImages(images, timeoutMinutes = 30) {
    // the writeFile allows us to call the script with arguments
    def waitScript = libraryResource('wait-for-images.sh')
    writeFile(file: './wait-for-images.sh', text: waitScript)
    sh 'chmod +x ./wait-for-images.sh'

    try {
        timeout(timeoutMinutes) {
            sh "./wait-for-images.sh ${images.join(' ')}"
        }
    } catch (e) {
        error("Timeout reached while pulling images [${images}]")
    }
}

def commitChange(commitMessage) {
    def script = """
    git add .
    if ! git diff-index --quiet HEAD --; then
        git commit -s -m '${commitMessage}'
    else
        echo 'No changes detected to commit'
        exit 1
    fi
    """.stripIndent()

    println "[edgeXReleaseDocs] committing change(s)"

    if(isDryRun()) {
        echo "git commit -s -m '${commitMessage}'"
    } else {
        sh script
    }
}

def createPR(branch, title, message, reviewers, pushCredentials = 'edgex-jenkins-ssh', ghCliCredentials='edgex-jenkins-github-personal-access-token') {
    commitChange(title)

    withCredentials([
        usernamePassword(
            credentialsId: ghCliCredentials,
            usernameVariable: 'GH_USER', passwordVariable: 'GH_TOKEN'
        )
    ]) {
        def prCreate = "gh pr create --base main --head ${branch} --title '${title}' --body '${message}' --reviewer '${reviewers}' --label 'ci,documentation'"

        if(isDryRun()) {
            echo "git push origin ${branch}"
            echo prCreate
        } else {
            sshagent(credentials: [pushCredentials]) {
                sh "git push origin ${branch}"
            }

            docker.image('ghcr.io/supportpal/github-gh-cli').inside('--entrypoint=') {
                sh prCreate
            }
        }
    }
}