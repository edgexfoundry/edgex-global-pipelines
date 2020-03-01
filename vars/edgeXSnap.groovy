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

/* Usage
edgeXSnap(
    jobType: 'build|snapshot|release',
    snapChannel: 'latest/edge|latest/snapshot', used for jobType = snapshot|release
)

Optional:

snapRevision, some revision number (2049)... used for jobType = release
snapName, name of the snap. If not provided, the snapcraft.yaml will be used to determine the name
snapBuilderImage, which docker image to use to build the snap
snapBase, the base directory where to build the snap
snapStoreLoginSettings, the name of the Jenkins settings file with login details
*/

def call(config = [:]) {
    def _arch = env.ARCH ?: 'amd64'
    def _snapBuilderImage = config.snapBuilderImage ?: "${DOCKER_REGISTRY}:10003/edgex-devops/edgex-snap-builder:latest"

    // TODO: find a better way to do this
    if(_arch == 'arm64') {
        _snapBuilderImage = "${DOCKER_REGISTRY}:10003/edgex-devops/edgex-snap-builder-${_arch}:latest"
    }

    def _snapBase     = config.snapBase ?: env.WORKSPACE
    def _jobType      = config.jobType ?: 'build'
    def _snapChannel  = config.snapChannel ?: 'latest/edge'
    def _snapRevision = config.snapRevision ?: ''
    def _snapName     = config.snapName

    def _snapStoreLoginSettings = config.snapStoreLoginSettings ?: 'EdgeX'

    def envVars = []

    envVars << "JOB_TYPE=${_jobType}"
    envVars << "SNAP_REVISION=${_snapRevision}"
    envVars << "SNAP_CHANNEL=${_snapChannel}"

    // find the snapcraft.yaml in the snapBase dir and return the name of the snap if not specified
    if(!_snapName) {
        _snapName = sh(script: "grep -Po '^name: \\K(.*)' \$(find ${_snapBase} | grep snapcraft.yaml)", returnStdout: true)
    }

    // if not null or empty
    if(_snapName) {
        envVars << "SNAP_NAME=${_snapName}"
    } else {
        error('Could not determine snap name. Please verify the snapcraft.yaml file and try again.')
    }

    def cfgFile = []

    if(env.SILO == 'production') {
        cfgFile = [configFile(fileId: _snapStoreLoginSettings, variable: 'SNAP_STORE_LOGIN')]
    }

    withEnv(envVars) {
        configFileProvider(cfgFile) {
            if(env.SILO == 'production') {
                sh 'cp $SNAP_STORE_LOGIN $WORKSPACE/edgex-snap-store-login'
            }

            sh """
            docker run --rm -u 0:0 --privileged \
              -v $WORKSPACE:/build \
              -w /build \
              -e JOB_TYPE \
              -e SNAP_REVISION \
              -e SNAP_CHANNEL \
              -e SNAP_NAME \
              ${_snapBuilderImage}
            """
        }
    }
}