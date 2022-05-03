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
 # edgeXInfraLFToolsSign

 ## Overview

 Shared library that wraps signing of git tags or files in a directory using
 [lftools](https://github.com/lfit/releng-lftools) and [Sigul](https://pagure.io/sigul).
 The signing is done inside a [docker image](https://github.com/edgexfoundry/ci-build-images/blob/lftools/Dockerfile)
 managed by the EdgeX DevOps team.
 
 **Important:** Signing git tags, requires the git tag to be an annotated tag. (`git tag -a`)

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 sigulConfig | false | str | Config file Id that contains the Sigul config. <br /><br />**Default**: `sigul-config` |
 sigulPassword | false | str | Config file Id that contains the Sigul password. <br /><br />**Default**: `sigul-password` |
 sigulPKI | false | str | Config file Id that contains the Sigul PKI information. <br /><br />**Default**: `sigul-pki` |
 lftoolsImageVersion | false | str | DevOps managed lftools image version tag. <br /><br />**Default**: `0.23.1-centos7` |
 command | true | str | Command to run. <br /><br />**Valid values**: `dir, git-tag` |
 directory | true | str | Required if command is `dir`. |
 version | true | str | Required if command is `git-tag`. |
 mode | false | str | lftools sign mode. <br /><br />**Default**: `parallel` |

 ## Usage

 ```groovy
 edgeXInfraLFToolsSign(command: 'git-tag', version: 'v2.0.0')
 ```

 ```groovy
 edgeXInfraLFToolsSign([command: 'dir', directory: 'MyDirectory'])
 ```
*/

def call(Map config = [:]) {
    stage('LF Tools Sigul') {

        // Sigul Configuration Files
        def _sigulConfig = config.sigulConfig ?: 'sigul-config'
        def _sigulPassword = config.sigulPassword ?: 'sigul-password'
        def _sigulPKI = config.sigulPKI ?: 'sigul-pki'

        // Expose lftools image version for override
        def _lftoolsImageVersion = config.lftoolsImageVersion ?: '0.23.1-centos7'

        def _command = config.command ?: ''
        def _directory = config.directory ?: ''
        def _mode = config.mode ?: 'parallel'
        def _version = config.version ?: ''
        // def _manifest = config.manifest ?: ''

        if(_command == 'dir' && !_directory) {
            error('Directory location (directory) is required to sign files in a directory.')
        }

        if(_command == 'git-tag' && !_version) {
            error('Version number (version) is required to sign a git teg.')
        }

        // TODO: Test signing containers in Fuji or later release
        // if(_command == 'container' && !_version && !_manifest) {
        //     throw new Exception('Version number (version) and docker manifest (manifest) are required to sign a docker container.')
        // }

        if(!_command) {
            error("Invalid command (command: ${_command}) provided for the edgeXInfraLFToolsSign function. (Valid values: dir, git-tag)")
        } else {
            def lftoolsImage = "nexus3.edgexfoundry.org:10003/edgex-lftools:${_lftoolsImageVersion}"
            
            docker.image(lftoolsImage).inside('-u 0:0') {
                configFileProvider([configFile(fileId: _sigulConfig, variable: 'SIGUL_CONFIG'),
                                configFile(fileId: _sigulPassword, variable: 'SIGUL_PASSWORD'),
                                configFile(fileId: _sigulPKI, variable: 'SIGUL_PKI')]) {
                    
                    echo 'Running global-jjb/shell/sigul-configuration.sh'
                    sh(script: libraryResource('global-jjb-shell/sigul-configuration.sh'))
                    sh 'mkdir /home/jenkins && mkdir /home/jenkins/sigul'
                    sh 'cp -R $HOME/sigul/* /home/jenkins/sigul/'
                    
                    echo 'Running global-jjb/shell/sigul-install.sh'
                    sh(script: libraryResource('global-jjb-shell/sigul-install.sh'))
                    
                    // Signing all files in a directory, requires the directory to not be empty
                    if(_command == 'dir') {
                        sh "lftools sign sigul -m \"${_mode}\" \"${_directory}\""
                        sh "chmod -R 775 ${_directory} && chown -R 1001:1001 ${_directory}"

                    // Signing git tags, requires the git tag to be an annotated tag.
                    // EX: git tag -a vtest -m 'Create test unsigned annotated tag'
                    } else if (_command == 'git-tag') {
                        sh 'git tag --list'
                        sh "lftools sign git-tag ${_version}"

                    // TODO: Test signing containers in Fuji or later release
                    // } else if (_command == 'container') {
                    //     sh "lftools sign container ${_manifest} ${_version}"
                    } else {
                        echo 'Running global-jjb/shell/sigul-configuration-cleanup.sh'
                        sh(script: libraryResource('global-jjb-shell/sigul-configuration-cleanup.sh'))
                        error("Invalid command (command: ${_command}) provided for the edgeXInfraLFToolsSign function. (Valid values: dir, git-tag)")
                    }

                    echo 'Running global-jjb/shell/sigul-configuration-cleanup.sh'
                    sh(script: libraryResource('global-jjb-shell/sigul-configuration-cleanup.sh'))
                }
            }
        }
    }
}