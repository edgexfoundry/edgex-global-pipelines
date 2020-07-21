import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore
import groovy.mock.interceptor.*
import spock.lang.*

public class EdgeXDockerSpec extends JenkinsPipelineSpecification {

    def edgeXDocker = null
    def environment = [:]
    def layerManifestJsonBefore
    def layerManifestJsonAfter
    def finalJson

    def setup() {
        edgeXDocker = loadPipelineScriptForTest('vars/edgeXDocker.groovy')
        edgeXDocker.getBinding().setVariable('env', environment)
        edgeXDocker.getBinding().setVariable('edgex', {})
        explicitlyMockPipelineVariable('out')

        // mock Docker layer manifest json for relabeling
        layerManifestJsonBefore = ["architecture":"amd64","config":["Hostname":"","Domainname":"","User":"","AttachStdin":false,"AttachStdout":false,"AttachStderr":false,"ExposedPorts":["49982/tcp":[]],"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","APP_PORT=49982"],"Cmd":null,"ArgsEscaped":true,"Image":"sha256:b7da8951d19af6e43e3943b2005ddaa91e756f12608832736307c80ae86893c6","Volumes":null,"WorkingDir":"","Entrypoint":["/device-mqtt","--cp=consul://edgex-core-consul:8500","--registry","--confdir=/res"],"OnBuild":null,"Labels":["arch":"amd64","copyright":"Copyright (c) 2020: IoTech Ltd","git_sha":"4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","license":"SPDX-License-Identifier: Apache-2.0","version":"1.2.0-dev.6"]],"container":"c4b1ea5a909bbe0130214f49692c84c69dc24c5fbda0e279a490ac593eb25e8b","container_config":["Hostname":"c4b1ea5a909b","Domainname":"","User":"","AttachStdin":false,"AttachStdout":false,"AttachStderr":false,"ExposedPorts":["49982/tcp":[]],"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","APP_PORT=49982"],"Cmd":["/bin/sh","-c","#(nop) ","LABEL version=1.2.0-dev.6"],"ArgsEscaped":true,"Image":"sha256:b7da8951d19af6e43e3943b2005ddaa91e756f12608832736307c80ae86893c6","Volumes":null,"WorkingDir":"","Entrypoint":["/device-mqtt","--cp=consul://edgex-core-consul:8500","--registry","--confdir=/res"],"OnBuild":null,"Labels":["arch":"amd64","copyright":"Copyright (c) 2020: IoTech Ltd","git_sha":"4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","license":"SPDX-License-Identifier: Apache-2.0","version":"1.2.0-dev.6"]],"created":"2020-05-13T15:00:33.187912771Z","docker_version":"18.06.1-ce","history":[["created":"2020-04-24T01:05:03.608058404Z","created_by":"/bin/sh -c #(nop) ADD file:b91adb67b670d3a6ff9463e48b7def903ed516be66fc4282d22c53e41512be49 in / "],["created":"2020-04-24T01:05:03.92860976Z","created_by":"/bin/sh -c #(nop)  CMD [\"/bin/sh\"]","empty_layer":true],["created":"2020-05-13T15:00:31.171074256Z","created_by":"/bin/sh -c #(nop)  ENV APP_PORT=49982","empty_layer":true],["created":"2020-05-13T15:00:31.350986142Z","created_by":"/bin/sh -c #(nop)  EXPOSE 49982","empty_layer":true],["created":"2020-05-13T15:00:31.846385353Z","created_by":"/bin/sh -c #(nop) COPY dir:04470546df63ad3205f5effa68ee102c754852f4ba651455a5bb5abf320cb3d0 in / "],["created":"2020-05-13T15:00:32.044628408Z","created_by":"/bin/sh -c #(nop) COPY file:b94ba7cd5544d88c31accc4efe75d2d15cf8bc01c20910ef606c805cc984075a in / "],["created":"2020-05-13T15:00:32.276438532Z","created_by":"/bin/sh -c #(nop) COPY file:6773549d7da6173ed8787ad12523b982c658e584dd952d6e81d711fabc33f962 in / "],["created":"2020-05-13T15:00:32.436122217Z","created_by":"/bin/sh -c #(nop)  LABEL license=SPDX-License-Identifier: Apache-2.0 copyright=Copyright (c) 2020: IoTech Ltd","empty_layer":true],["created":"2020-05-13T15:00:32.641016434Z","created_by":"/bin/sh -c #(nop)  ENTRYPOINT [\"/device-mqtt\" \"--cp=consul://edgex-core-consul:8500\" \"--registry\" \"--confdir=/res\"]","empty_layer":true],["created":"2020-05-13T15:00:32.814519093Z","created_by":"/bin/sh -c #(nop)  LABEL arch=amd64","empty_layer":true],["created":"2020-05-13T15:00:33.013443391Z","created_by":"/bin/sh -c #(nop)  LABEL git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","empty_layer":true],["created":"2020-05-13T15:00:33.187912771Z","created_by":"/bin/sh -c #(nop)  LABEL version=1.2.0-dev.6","empty_layer":true]],"os":"linux","rootfs":["type":"layers","diff_ids":["sha256:3e207b409db364b595ba862cdc12be96dcdad8e36c59a03b7b3b61c946a5741a","sha256:c1edd3b3fa08e7c472be0a1114d3cce619ffc955ab4d82b5ee93ddd5d0c622b4","sha256:6586dd444b1a9882fa163436f377948347b6f9e3b02fd7aa5aeb2bff9ae884d9","sha256:e991526dc478bc5c1039461a8679787985231d38641e4ba17407c87c9b7fb38c"]]]
        layerManifestJsonAfter = ["architecture":"amd64","config":["Hostname":"","Domainname":"","User":"","AttachStdin":false,"AttachStdout":false,"AttachStderr":false,"ExposedPorts":["49982/tcp":[]],"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","APP_PORT=49982"],"Cmd":null,"ArgsEscaped":true,"Image":"sha256:b7da8951d19af6e43e3943b2005ddaa91e756f12608832736307c80ae86893c6","Volumes":null,"WorkingDir":"","Entrypoint":["/device-mqtt","--cp=consul://edgex-core-consul:8500","--registry","--confdir=/res"],"OnBuild":null,"Labels":["arch":"amd64","copyright":"Copyright (c) 2020: IoTech Ltd","git_sha":"4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","license":"SPDX-License-Identifier: Apache-2.0","version":"1.2.1"]],"container":"c4b1ea5a909bbe0130214f49692c84c69dc24c5fbda0e279a490ac593eb25e8b","container_config":["Hostname":"c4b1ea5a909b","Domainname":"","User":"","AttachStdin":false,"AttachStdout":false,"AttachStderr":false,"ExposedPorts":["49982/tcp":[]],"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","APP_PORT=49982"],"Cmd":["/bin/sh","-c","#(nop) ","LABEL version=1.2.1"],"ArgsEscaped":true,"Image":"sha256:b7da8951d19af6e43e3943b2005ddaa91e756f12608832736307c80ae86893c6","Volumes":null,"WorkingDir":"","Entrypoint":["/device-mqtt","--cp=consul://edgex-core-consul:8500","--registry","--confdir=/res"],"OnBuild":null,"Labels":["arch":"amd64","copyright":"Copyright (c) 2020: IoTech Ltd","git_sha":"4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","license":"SPDX-License-Identifier: Apache-2.0","version":"1.2.1"]],"created":"2020-05-13T15:00:33.187912771Z","docker_version":"18.06.1-ce","history":[["created":"2020-04-24T01:05:03.608058404Z","created_by":"/bin/sh -c #(nop) ADD file:b91adb67b670d3a6ff9463e48b7def903ed516be66fc4282d22c53e41512be49 in / "],["created":"2020-04-24T01:05:03.92860976Z","created_by":"/bin/sh -c #(nop)  CMD [\"/bin/sh\"]","empty_layer":true],["created":"2020-05-13T15:00:31.171074256Z","created_by":"/bin/sh -c #(nop)  ENV APP_PORT=49982","empty_layer":true],["created":"2020-05-13T15:00:31.350986142Z","created_by":"/bin/sh -c #(nop)  EXPOSE 49982","empty_layer":true],["created":"2020-05-13T15:00:31.846385353Z","created_by":"/bin/sh -c #(nop) COPY dir:04470546df63ad3205f5effa68ee102c754852f4ba651455a5bb5abf320cb3d0 in / "],["created":"2020-05-13T15:00:32.044628408Z","created_by":"/bin/sh -c #(nop) COPY file:b94ba7cd5544d88c31accc4efe75d2d15cf8bc01c20910ef606c805cc984075a in / "],["created":"2020-05-13T15:00:32.276438532Z","created_by":"/bin/sh -c #(nop) COPY file:6773549d7da6173ed8787ad12523b982c658e584dd952d6e81d711fabc33f962 in / "],["created":"2020-05-13T15:00:32.436122217Z","created_by":"/bin/sh -c #(nop)  LABEL license=SPDX-License-Identifier: Apache-2.0 copyright=Copyright (c) 2020: IoTech Ltd","empty_layer":true],["created":"2020-05-13T15:00:32.641016434Z","created_by":"/bin/sh -c #(nop)  ENTRYPOINT [\"/device-mqtt\" \"--cp=consul://edgex-core-consul:8500\" \"--registry\" \"--confdir=/res\"]","empty_layer":true],["created":"2020-05-13T15:00:32.814519093Z","created_by":"/bin/sh -c #(nop)  LABEL arch=amd64","empty_layer":true],["created":"2020-05-13T15:00:33.013443391Z","created_by":"/bin/sh -c #(nop)  LABEL git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4","empty_layer":true],["created":"2020-05-13T15:00:33.187912771Z","created_by":"/bin/sh -c #(nop)  LABEL version=1.2.1","empty_layer":true]],"os":"linux","rootfs":["type":"layers","diff_ids":["sha256:3e207b409db364b595ba862cdc12be96dcdad8e36c59a03b7b3b61c946a5741a","sha256:c1edd3b3fa08e7c472be0a1114d3cce619ffc955ab4d82b5ee93ddd5d0c622b4","sha256:6586dd444b1a9882fa163436f377948347b6f9e3b02fd7aa5aeb2bff9ae884d9","sha256:e991526dc478bc5c1039461a8679787985231d38641e4ba17407c87c9b7fb38c"]]]
    }

    def "Test build [Should] call docker build with expected arguments [When] no BUILD_SCRIPT DOCKER_BUILD_ARGS" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('GIT_COMMIT', 'MyGitCommit')
            edgeXDocker.getBinding().setVariable('ARCH', 'MyArch')
            edgeXDocker.getBinding().setVariable('DOCKER_FILE_PATH', 'MyDockerFilePath')
            edgeXDocker.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')

        when:
            edgeXDocker.build('MyDockerImageName')
        then:
            1 * getPipelineMock("docker.build").call(['MyDockerImageName', "-f MyDockerFilePath  --build-arg ARCH=MyArch  --label 'git_sha=MyGitCommit' --label 'arch=MyArch' MyDockerBuildContext"])
    }

    def "Test build [Should] call docker build with expected arguments [When] BUILD_SCRIPT DOCKER_BUILD_ARGS" () {
        setup:
            def environmentVariables = [
                'BUILD_SCRIPT': 'MyBuildScript',
                'DOCKER_BUILD_ARGS': 'MyArg1,MyArg2,MyArg3',
                'http_proxy': 'MyHttpProxy',
                'VERSION': 'MyVersion',
                'ARCH': 'MyArch'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('GIT_COMMIT', 'MyGitCommit')
            edgeXDocker.getBinding().setVariable('ARCH', 'MyArch')
            edgeXDocker.getBinding().setVariable('DOCKER_FILE_PATH', 'MyDockerFilePath')
            edgeXDocker.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')
            edgeXDocker.getBinding().setVariable('BUILD_SCRIPT', 'MyBuildScript')
            edgeXDocker.getBinding().setVariable('VERSION', 'MyVersion')
        when:
            edgeXDocker.build('MyDockerImageName')
        then:
            1 * getPipelineMock("docker.build").call(['MyDockerImageName', "-f MyDockerFilePath  --build-arg MAKE='MyBuildScript' --build-arg ARCH=MyArch --build-arg http_proxy --build-arg https_proxy --build-arg MyArg1 --build-arg MyArg2 --build-arg MyArg3  --label 'git_sha=MyGitCommit' --label 'arch=MyArch' --label 'version=MyVersion' MyDockerBuildContext"])
    }

    def "Test push [Should] call image push with expected arguments [When] VERSION SEMVER_BRANCH DOCKER_CUSTOM_TAGS" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'VERSION': 'MyVersion',
                'SEMVER_BRANCH': 'MySemverBranch',
                'DOCKER_CUSTOM_TAGS': 'MyDockerCustomTags',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName')
        then:
            1 * getPipelineMock('DockerImageMock.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock.push').call('latest')
            1 * getPipelineMock('DockerImageMock.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock.push').call('MyVersion')
            1 * getPipelineMock('DockerImageMock.push').call('MyDockerCustomTags')
            1 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10004' == _arguments[0][0]
            }
    }

    def "Test push [Should] call push to correct registry and port [When] nexusRepo is snapshot" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName', true, 'snapshot')
        then:
            1 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10003' == _arguments[0][0]
            }
    }

    def "Test push [Should] call push to correct registry and port [When] nexusRepo is snapshots" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName', true, 'snapshots')
        then:
            1 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10003' == _arguments[0][0]
            }
    }

    def "Test push [Should] call push to correct registry and port [When] nexusRepo is release" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName', true, 'release')
        then:
            1 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10002' == _arguments[0][0]
            }
    }

    def "Test pushAll [Should] call push to correct registry and port [When] nexusRepo is staging when latest is true" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'VERSION': 'MyVersion',
                'SEMVER_BRANCH': 'MySemverBranch',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName-1') >> explicitlyMockPipelineVariable('DockerImageMock-1')
            getPipelineMock('docker.image')('MyDockerImageName-2') >> explicitlyMockPipelineVariable('DockerImageMock-2')
            getPipelineMock('docker.image')('MyDockerImageName-3') >> explicitlyMockPipelineVariable('DockerImageMock-3')
        when:
            edgeXDocker.pushAll([
                [ image: 'MyDockerImageName-1', dockerfile: 'cmd/MyDockerfile' ],
                [ image: 'MyDockerImageName-2', dockerfile: 'cmd/MyDockerfile' ],
                [ image: 'MyDockerImageName-3', dockerfile: 'cmd/MyDockerfile' ],
            ], true, 'staging')
        then:
            1 * getPipelineMock('DockerImageMock-1.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-1.push').call('latest')
            1 * getPipelineMock('DockerImageMock-1.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-1.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-1.push').call('MyVersion')

            1 * getPipelineMock('DockerImageMock-2.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-2.push').call('latest')
            1 * getPipelineMock('DockerImageMock-2.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-2.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-2.push').call('MyVersion')

            1 * getPipelineMock('DockerImageMock-3.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-3.push').call('latest')
            1 * getPipelineMock('DockerImageMock-3.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-3.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-3.push').call('MyVersion')

            3 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10004' == _arguments[0][0]
            }
    }

    def "Test pushAll [Should] call push to correct registry and port [When] nexusRepo is staging and latest flag is false" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'VERSION': 'MyVersion',
                'SEMVER_BRANCH': 'MySemverBranch',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('MyDockerImageName-1') >> explicitlyMockPipelineVariable('DockerImageMock-1')
            getPipelineMock('docker.image')('MyDockerImageName-2') >> explicitlyMockPipelineVariable('DockerImageMock-2')
            getPipelineMock('docker.image')('MyDockerImageName-3') >> explicitlyMockPipelineVariable('DockerImageMock-3')
        when:
            edgeXDocker.pushAll([
                    [ image: 'MyDockerImageName-1', dockerfile: 'cmd/MyDockerfile' ],
                    [ image: 'MyDockerImageName-2', dockerfile: 'cmd/MyDockerfile' ],
                    [ image: 'MyDockerImageName-3', dockerfile: 'cmd/MyDockerfile' ],
            ], false, 'staging')
        then:
            1 * getPipelineMock('DockerImageMock-1.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-1.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-1.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-1.push').call('MyVersion')

            1 * getPipelineMock('DockerImageMock-2.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-2.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-2.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-2.push').call('MyVersion')

            1 * getPipelineMock('DockerImageMock-3.push').call('MyGitCommit')
            1 * getPipelineMock('DockerImageMock-3.push').call('MySemverBranch')
            1 * getPipelineMock('DockerImageMock-3.push').call('MyGitCommit-MyVersion')
            1 * getPipelineMock('DockerImageMock-3.push').call('MyVersion')

            3 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10004' == _arguments[0][0]
            }
    }

    def "Test finalImageName [Should] return expected [When] DOCKER_REGISTRY_NAMESPACE" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY_NAMESPACE': 'MyDockerRegistryNamespace'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY_NAMESPACE', 'MyDockerRegistryNamespace')
        expect:
            edgeXDocker.finalImageName('MyImageName') == expectedResult
        where:
            expectedResult << [
                'MyDockerRegistryNamespace/MyImageName'
            ]
    }

    def "Test finalImageName [Should] return expected [When] DOCKER_REGISTRY_NAMESPACE is /" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY_NAMESPACE': '/'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY_NAMESPACE', 'MyDockerRegistryNamespace')
        expect:
            edgeXDocker.finalImageName('MyImageName') == expectedResult
        where:
            expectedResult << [
                'MyImageName'
            ]
    }

    def "Test finalImageName [Should] return expected [When] no DOCKER_REGISTRY_NAMESPACE" () {
        setup:
        expect:
            edgeXDocker.finalImageName('MyImageName') == expectedResult
        where:
            expectedResult << [
                'MyImageName'
            ]
    }

    def "Test cleanImageUrl [Should] return no http:// or https:// [When] called" () {
        setup:
        expect:
            edgeXDocker.cleanImageUrl('http://nexus3.edgexfoundry.org:10001/docker-foo-bar:master') == 'nexus3.edgexfoundry.org:10001/docker-foo-bar:master'
            edgeXDocker.cleanImageUrl('https://nexus3.edgexfoundry.org:10001/docker-foo-bar:master') == 'nexus3.edgexfoundry.org:10001/docker-foo-bar:master'
            edgeXDocker.cleanImageUrl('nexus3.edgexfoundry.org:10001/docker-foo-bar:master') == 'nexus3.edgexfoundry.org:10001/docker-foo-bar:master'
            edgeXDocker.cleanImageUrl('edgexfoundry/docker-foo-bar:master') == 'edgexfoundry/docker-foo-bar:master'
    }

    def "Test parse [Should] return expected [When] called" () {
        setup:
        expect:
            [
                'http://nexus3.edgexfoundry.org:10001/docker-foo-bar:master',
                'https://nexus3.edgexfoundry.org:10002/docker-foo-bar:master',
                'nexus3.edgexfoundry.org:10003/docker-foo-bar:master',
                'nexus3.edgexfoundry.org:10004/edgex-devops/docker-foo-bar',
                'example.com/extra/docker-foo-bar:master',
                'edgexfoundry/docker-foo-bar:v1.1.2',
                'edgexfoundry/docker-foo-bar',
                'docker.io/edgexfoundry/docker-foo-bar:v1.1.2',
                'python:3-alpine',
                'node'
            ].collect { edgeXDocker.parse(it) } == expectedResult
        where:
            expectedResult = [
                [host:'nexus3.edgexfoundry.org:10001', fullImage:'docker-foo-bar:master', namespace:null, image:'docker-foo-bar', tag:'master'],
                [host:'nexus3.edgexfoundry.org:10002', fullImage:'docker-foo-bar:master', namespace:null, image:'docker-foo-bar', tag:'master'],
                [host:'nexus3.edgexfoundry.org:10003', fullImage:'docker-foo-bar:master', namespace:null, image:'docker-foo-bar', tag:'master'],
                [host:'nexus3.edgexfoundry.org:10004', fullImage:'edgex-devops/docker-foo-bar', namespace:'edgex-devops', image:'docker-foo-bar', tag:'latest'],
                [host:'example.com', fullImage:'extra/docker-foo-bar:master', namespace:'extra', image:'docker-foo-bar', tag:'master'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar:v1.1.2', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'v1.1.2'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'latest'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar:v1.1.2', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'v1.1.2'],
                [host:'docker.io', fullImage:'python:3-alpine', namespace:null, image:'python', tag:'3-alpine'],
                [host:'docker.io', fullImage:'node', namespace:null, image:'node', tag:'latest']
            ]
    }

    def "Test toImageStr [Should] return expected [When] called" () {
        setup:
        expect:
            [
                [host:'nexus3.edgexfoundry.org:10001', fullImage:'docker-foo-bar:master', namespace:null, image:'docker-foo-bar', tag:'master'],
                [host:'nexus3.edgexfoundry.org:10002', fullImage:'docker-foo-bar:latest', namespace:null, image:'docker-foo-bar', tag:'latest'],
                [host:'nexus3.edgexfoundry.org:10003', fullImage:'edgex-devops/docker-foo-bar', namespace:'edgex-devops', image:'docker-foo-bar', tag:'latest'],
                [host:'example.com', fullImage:'extra/docker-foo-bar:master', namespace:'extra', image:'docker-foo-bar', tag:'master'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar:v1.1.2', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'v1.1.2'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'latest'],
                [host:'docker.io', fullImage:'edgexfoundry/docker-foo-bar:v1.1.2', namespace:'edgexfoundry', image:'docker-foo-bar', tag:'v1.1.2'],
                [host:'docker.io', fullImage:'python:3-alpine', namespace:null, image:'python', tag:'3-alpine'],
                [host:null, fullImage:'node', namespace:null, image:'node', tag:null]
            ].collect { edgeXDocker.toImageStr(it) } == expectedResult
        where:
            expectedResult = [
                'nexus3.edgexfoundry.org:10001/docker-foo-bar:master',
                'nexus3.edgexfoundry.org:10002/docker-foo-bar:latest',
                'nexus3.edgexfoundry.org:10003/edgex-devops/docker-foo-bar:latest',
                'example.com/extra/docker-foo-bar:master',
                'docker.io/edgexfoundry/docker-foo-bar:v1.1.2',
                'docker.io/edgexfoundry/docker-foo-bar:latest',
                'docker.io/edgexfoundry/docker-foo-bar:v1.1.2',
                'docker.io/python:3-alpine',
                'docker.io/node'
            ]
    }

    def "Test getDockerTags [Should] return a default set of tags [When] latest is true and no env vars" () {
        setup:
        expect:
            edgeXDocker.getDockerTags() == ['latest']
    }

    def "Test getDockerTags [Should] return an empty list [When] latest is false and no env vars" () {
        setup:
        expect:
            edgeXDocker.getDockerTags(false) == []
    }

    def "Test getDockerTags [Should] return tags with latest [When] called with all env vars" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyGitCommit',
                'VERSION': 'MyVersion',
                'SEMVER_BRANCH': 'MySemverBranch',
                'DOCKER_CUSTOM_TAGS': 'MyDockerCustomTags MyOtherDockerCustomTag',
                'BUILD_EXPERIMENTAL_DOCKER_IMAGE': 'true',
                'BUILD_STABLE_DOCKER_IMAGE': 'true'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeXDocker.getDockerTags() == expectedResult
        where:
        expectedResult = [
            'MyGitCommit',
            'latest',
            'MyVersion',
            'MyGitCommit-MyVersion',
            'MySemverBranch',
            'experimental',
            'stable',
            'MyDockerCustomTags',
            'MyOtherDockerCustomTag'
        ]
    }
    
    def "Test replaceDockerLabel [Should] return expected [When] called" () {
        setup:
        expect:
        [
            ['LABEL version=1.2.0-dev.1', 'version', '1.2.1'],
            ['LABEL git_sha=previousCommit', 'git_sha', 'newCommitSha']
        ].collect { edgeXDocker.replaceDockerLabel(it[0], it[1], it[2]) } == expectedResult
        where:
        expectedResult = [
            'LABEL version=1.2.1',
            'LABEL git_sha=newCommitSha'
        ]
    }

    def "Test getDockerConfigJson [Should] return expected [When] called" () {
        setup:
            getPipelineMock('readJSON').call(file: 'manifest.json') >> [ Config: ['87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json'] ]
            getPipelineMock('readJSON').call(file: './87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json') >> [ Mock: true ]
        expect:
            edgeXDocker.getDockerConfigJson('manifest.json') == expectedResult
        where:
            expectedResult = [
                filename: '87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json',
                json: [ Mock: true ]
            ]
    }

    def "Test relabel [Should] call expected [When] pullImage is true" () {
        setup:
            explicitlyMockPipelineStep('dir')
            explicitlyMockPipelineStep('getTmpDir')
            explicitlyMockPipelineStep('getDockerConfigJson')
            getPipelineMock('getTmpDir')() >> 'ci-abc12'

            getPipelineMock('readJSON').call(file: './manifest.json') >> [ Config: ['87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json'] ]
            getPipelineMock('readJSON').call(file: './87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json') >> layerManifestJsonBefore
        when:
            finalJson = edgeXDocker.relabel(
                'nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'docker-device-mqtt-go:promoting',
                [version: '1.2.1'],
                true
            )
        then:
            1 * getPipelineMock('sh').call('docker pull nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('sh').call('docker save -o ./image.tar nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('sh').call('tar -xvf image.tar && rm -rf image.tar')
            1 * getPipelineMock('writeJSON').call(file: '87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json', json: layerManifestJsonAfter)
            1 * getPipelineMock('sh').call('tar -cvf image.tar *')
            1 * getPipelineMock('sh').call('docker load -i image.tar')
            1 * getPipelineMock('sh').call('docker tag nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4 docker-device-mqtt-go:promoting')

            assert finalJson == layerManifestJsonAfter
    }

    def "Test relabel [Should] call expected [When] pullImage is false" () {
        setup:
            explicitlyMockPipelineStep('dir')
            explicitlyMockPipelineStep('getTmpDir')
            explicitlyMockPipelineStep('getDockerConfigJson')
            getPipelineMock('getTmpDir')() >> 'ci-abc12'

            getPipelineMock('readJSON').call(file: './manifest.json') >> [ Config: ['87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json'] ]
            getPipelineMock('readJSON').call(file: './87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json') >> layerManifestJsonBefore
        when:
            finalJson = edgeXDocker.relabel(
                'nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'docker-device-mqtt-go:promoting',
                [version: '1.2.1'],
                false
            )
        then:
            0 * getPipelineMock('sh').call('docker pull nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('sh').call('docker save -o ./image.tar nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('sh').call('tar -xvf image.tar && rm -rf image.tar')
            1 * getPipelineMock('writeJSON').call(file: '87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json', json: layerManifestJsonAfter)
            1 * getPipelineMock('sh').call('tar -cvf image.tar *')
            1 * getPipelineMock('sh').call('docker load -i image.tar')
            1 * getPipelineMock('sh').call('docker tag nexus3.edgexfoundry.org:10004/docker-device-mqtt-go:4f542d150f63fd84c4c4e03c791e07fdb7c9aef4 docker-device-mqtt-go:promoting')

            // returning the json is not required, only for unit tests
            assert finalJson == layerManifestJsonAfter
    }

    // Ignoring this test for now as promotion is on hold or will be removed
    @Ignore
    def "Test promote [Should] call expected [When] promoting to snapshots" () {
        setup:
            explicitlyMockPipelineVariable("getDockerTags")
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('push')
            def environmentVariables = [
                'GIT_COMMIT': '4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'VERSION': null,
                'SEMVER_BRANCH': 'master',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'DOCKER_REGISTRY_NAMESPACE': 'promo-time', // this is needed to mock the withEnv
                'DOCKER_PROMOTION_NAMESPACE': 'promo-time'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY_NAMESPACE', 'promo-time')
            getPipelineMock('docker.image')("edgexfoundry/docker-device-mqtt-go") >> explicitlyMockPipelineVariable('edgexfoundry/docker-device-mqtt-go')
            getPipelineMock('docker.image')("edgexfoundry/docker-device-mqtt-go-arm64") >> explicitlyMockPipelineVariable('edgexfoundry/docker-device-mqtt-go-arm64')
        when:
            edgeXDocker.promote([
                'edgexfoundry/docker-device-mqtt-go',
                'edgexfoundry/docker-device-mqtt-go-arm64'
            ], 'snapshot', environmentVariables.GIT_COMMIT, environmentVariables.VERSION)
        then:
            2 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://nexus3.edgexfoundry.org:10003/promo-time' == _arguments[0][0]
            }
            1 * getPipelineMock('edgexfoundry/docker-device-mqtt-go.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('edgexfoundry/docker-device-mqtt-go-arm64.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
    }

    def "Test promote [Should] call expected [When] promoting to staging" () {
        setup:
            explicitlyMockPipelineVariable("getDockerTags")
            explicitlyMockPipelineVariable("getPreviousCommit")

            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('push')

            explicitlyMockPipelineStep('dir')
            explicitlyMockPipelineStep('getTmpDir')
            explicitlyMockPipelineStep('getDockerConfigJson')
            getPipelineMock('getTmpDir')() >> 'ci-abc12'

            getPipelineMock('readJSON').call(file: './manifest.json') >> [ Config: ['87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json'] ]
            getPipelineMock('readJSON').call(file: './87883221c4438cff2ac9a3eb4ee8867ae92ae00401fa33dcda33bef47ddc50ec.json') >> layerManifestJsonBefore

            def environmentVariables = [
                    'GIT_COMMIT': '4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                    'VERSION': '1.33.3',
                    'SEMVER_BRANCH': 'master',
                    'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')("docker-device-mqtt-go") >> explicitlyMockPipelineVariable('docker-device-mqtt-go')
            getPipelineMock('docker.image')("docker-device-mqtt-go-arm64") >> explicitlyMockPipelineVariable('docker-device-mqtt-go-arm64')
        when:
            edgeXDocker.promote([
                'edgexfoundry/docker-device-mqtt-go',
                'edgexfoundry/docker-device-mqtt-go-arm64'
            ], 'staging', environmentVariables.GIT_COMMIT, environmentVariables.VERSION)
        then:
            2 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://nexus3.edgexfoundry.org:10004' == _arguments[0][0]
            }

            1 * getPipelineMock('docker-device-mqtt-go.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('docker-device-mqtt-go.push').call('latest')
            1 * getPipelineMock('docker-device-mqtt-go.push').call('1.33.3')
            1 * getPipelineMock('docker-device-mqtt-go.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4-1.33.3')
            1 * getPipelineMock('docker-device-mqtt-go.push').call('master')

            1 * getPipelineMock('docker-device-mqtt-go-arm64.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4')
            1 * getPipelineMock('docker-device-mqtt-go-arm64.push').call('latest')
            1 * getPipelineMock('docker-device-mqtt-go-arm64.push').call('1.33.3')
            1 * getPipelineMock('docker-device-mqtt-go-arm64.push').call('4f542d150f63fd84c4c4e03c791e07fdb7c9aef4-1.33.3')
            1 * getPipelineMock('docker-device-mqtt-go-arm64.push').call('master')
    }

    def "Test generateDockerComposeForBuild [Should] return expected [When] called with labels" () {
        setup:

        expect:
            def dockers = [
                [image: 'image-1-go', dockerfile: 'cmd/image-1/Dockerfile'],
                [image: 'image-2-go', dockerfile: 'cmd/image-2/Dockerfile']
            ]

            def labels = [
                'git_sha': '4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'version': '1.21.0'
            ]

            edgeXDocker.generateDockerComposeForBuild(dockers, labels, 'docker-') == expectedResult
        where:
            expectedResult = '''
version: '3.7'
services:

  image-1-go:
    build:
      context: .
      dockerfile: cmd/image-1/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - version=1.21.0
      args:
        - BUILDER_BASE
    image: docker-image-1-go

  image-2-go:
    build:
      context: .
      dockerfile: cmd/image-2/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - version=1.21.0
      args:
        - BUILDER_BASE
    image: docker-image-2-go
'''
    }

    def "Test generateDockerComposeForBuild [Should] return expected [When] called without labels" () {
        setup:

        expect:
            def dockers = [
                [image: 'image-1-go', dockerfile: 'cmd/image-1/Dockerfile'],
                [image: 'image-2-go', dockerfile: 'cmd/image-2/Dockerfile']
            ]

            edgeXDocker.generateDockerComposeForBuild(dockers, null, 'prefix-') == expectedResult
        where:
            expectedResult = '''
version: '3.7'
services:

  image-1-go:
    build:
      context: .
      dockerfile: cmd/image-1/Dockerfile
      
      args:
        - BUILDER_BASE
    image: prefix-image-1-go

  image-2-go:
    build:
      context: .
      dockerfile: cmd/image-2/Dockerfile
      
      args:
        - BUILDER_BASE
    image: prefix-image-2-go
'''
    }

    def "Test generateServiceYaml [Should] return expected [When] called with labels" () {
        setup:

        expect:
            def labels = [
                'label_1': 'First-Label',
                'label_2': 'Second-Label'
            ]
            edgeXDocker.generateServiceYaml('image-1-go', 'prefix-', 'cmd/image-1/Dockerfile', labels) == expectedResult
        where:
            expectedResult = '''
  image-1-go:
    build:
      context: .
      dockerfile: cmd/image-1/Dockerfile
      labels:
        - label_1=First-Label
        - label_2=Second-Label
      args:
        - BUILDER_BASE
    image: prefix-image-1-go'''
    }

    def "Test buildInParallel [Should] build expected docker images [When] called for amd64" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('writeFile')

            def environmentVariables = [
                'GIT_COMMIT': '4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'VERSION': '1.33.3',
                'ARCH': 'x86_64'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('docker.image')("nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest") >> explicitlyMockPipelineVariable('nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest')

            getPipelineMock('sh')([
                returnStatus: true,
                script: 'docker-compose build --help | grep parallel'
            ]) >> {
                0
            }
        when:
            edgeXDocker.buildInParallel([
                [image: 'image-1-go', dockerfile: 'cmd/image-1/Dockerfile'],
                [image: 'image-2-go', dockerfile: 'cmd/image-2/Dockerfile']
            ], 'docker-', 'ci-base-image')
        then:
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                _arguments[0][0] == 'BUILDER_BASE=ci-base-image'
            }

            1 * getPipelineMock("sh").call('docker-compose -f ./docker-compose-build.yml build --parallel')

            1 * getPipelineMock("writeFile").call(['file': './docker-compose-build.yml', 'text': '''
version: '3.7'
services:

  image-1-go:
    build:
      context: .
      dockerfile: cmd/image-1/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - arch=x86_64
        - version=1.33.3
      args:
        - BUILDER_BASE
    image: docker-image-1-go

  image-2-go:
    build:
      context: .
      dockerfile: cmd/image-2/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - arch=x86_64
        - version=1.33.3
      args:
        - BUILDER_BASE
    image: docker-image-2-go
'''])
    }

    def "Test buildInParallel [Should] build expected docker images [When] called for arm64" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('writeFile')

            def environmentVariables = [
                'GIT_COMMIT': '4f542d150f63fd84c4c4e03c791e07fdb7c9aef4',
                'VERSION': '1.33.3',
                'ARCH': 'arm64'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('docker.image')("nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose-arm64:latest") >> explicitlyMockPipelineVariable('nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose-arm64:latest')

            getPipelineMock('sh')([
                returnStatus: true,
                script: 'docker-compose build --help | grep parallel'
            ]) >> {
                0
            }
        when:
            edgeXDocker.buildInParallel([
                [image: 'image-1-go', dockerfile: 'cmd/image-1/Dockerfile'],
                [image: 'image-2-go', dockerfile: 'cmd/image-2/Dockerfile']
            ], 'docker-', 'ci-base-image-arm64')
        then:
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                _arguments[0][0] == 'BUILDER_BASE=ci-base-image-arm64'
            }

            1 * getPipelineMock("sh").call('docker-compose -f ./docker-compose-build.yml build --parallel')

            1 * getPipelineMock("writeFile").call(['file': './docker-compose-build.yml', 'text': '''
version: '3.7'
services:

  image-1-go:
    build:
      context: .
      dockerfile: cmd/image-1/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - arch=arm64
        - version=1.33.3
      args:
        - BUILDER_BASE
    image: docker-image-1-go-arm64

  image-2-go:
    build:
      context: .
      dockerfile: cmd/image-2/Dockerfile
      labels:
        - git_sha=4f542d150f63fd84c4c4e03c791e07fdb7c9aef4
        - arch=arm64
        - version=1.33.3
      args:
        - BUILDER_BASE
    image: docker-image-2-go-arm64
'''])
    }

    def "Test buildInParallel [Should] throw error [When] docker-compose does not support parallel" () {
        setup:
            explicitlyMockPipelineStep('error')

            getPipelineMock('docker.image')("nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest") >> explicitlyMockPipelineVariable('nexus3.edgexfoundry.org:10003/edgex-devops/edgex-compose:latest')

            getPipelineMock('sh')([
                returnStatus: true,
                script: 'docker-compose build --help | grep parallel'
            ]) >> {
                1
            }
        when:
            edgeXDocker.buildInParallel([
                [image: 'image-1-go', dockerfile: 'cmd/image-1/Dockerfile'],
                [image: 'image-2-go', dockerfile: 'cmd/image-2/Dockerfile']
            ], 'docker-', 'ci-base-image')
        then:
            1 * getPipelineMock('error').call('[edgeXDocker] --parallel build is not supported in this version of docker-compose')
    }
}
