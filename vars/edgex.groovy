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
    def releaseStreams = [/.*master/, /.*california/, /.*delhi/, /.*edinburgh/]

    (branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true))
}


def dirsChanged(String directory) {
    // If there was no previous successful build (as in building for first time) will return true.
    def diffCount = "0"
    if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT != null) {
        diffCount = sh(returnStdout: true, script: "git diff --name-only ${env.GIT_COMMIT} ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT} | grep ${directory} | wc -l").trim()
        // Will return true for manual build
        if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT == env.GIT_COMMIT) {
            diffCount = "1"
        }
    } else {
        diffCount = "1"
    }

    if (diffCount != "0") {
        return true
    } else {
        return false
    }
}