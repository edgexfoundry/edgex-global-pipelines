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

def call(branchName = env.GIT_BRANCH, body) {
    // what defines a main release branch
    def releaseStreams = [/.*master/, /.*california/, /.*delhi/, /.*edinburgh/] //, /.*git-semver/

    if(branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true)) {
        println "[edgeXPRStage] Current branch [${branchName}] IS a valid release branch, skipping code..."
    } else {
        println "[edgeXPRStage] Not a release [${branchName}] branch, must be in a feature branch or PR"
        body()
    }
}