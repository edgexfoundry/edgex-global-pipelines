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
 # edgeXSnap
 
 ## Overview

 ⚠️ Deprecated Warning... Snaps are no longer being built as part of the Jenkins pipeline. They are being built and tested using GitHub Actions and are managed by the team at Canonical. DO NOT USE ⚠️

 Wrapper around resources/snap-build.sh script. No parameters required.

 ## Usage

 ```groovy
 edgeXSnap()
 ```
*/

def call(config = [:]) {
    sh(script: libraryResource('snap-build.sh'))
}