import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore
import groovy.mock.interceptor.*
import spock.lang.*

public class EdgeXDockerSpec extends JenkinsPipelineSpecification {

    def edgeXDocker = null
    def environment = [:]

    def setup() {
        edgeXDocker = loadPipelineScriptForTest('vars/edgeXDocker.groovy')
        edgeXDocker.getBinding().setVariable('env', environment)
        explicitlyMockPipelineVariable('out')
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
                'DOCKER_CUSTOM_TAGS': 'MyDockerCustomTags'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('GIT_COMMIT', 'MyGitCommit')
            edgeXDocker.getBinding().setVariable('VERSION', 'MyVersion')
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY', 'MyDockerRegistry')
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName')
        then:
            1 * getPipelineMock('DockerImageMock.push').call('latest')
            1 * getPipelineMock('DockerImageMock.push').call('MyGitCommit')
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
                'GIT_COMMIT': 'MyGitCommit'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY', 'MyDockerRegistry')
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
                'GIT_COMMIT': 'MyGitCommit'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY', 'MyDockerRegistry')
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
                'GIT_COMMIT': 'MyGitCommit'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('DOCKER_REGISTRY', 'MyDockerRegistry')
            getPipelineMock('docker.image')('MyDockerImageName') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXDocker.push('MyDockerImageName', true, 'release')
        then:
            1 * getPipelineMock('docker.withRegistry').call(_) >> { _arguments ->
                assert 'https://MyDockerRegistry:10002' == _arguments[0][0]
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
}
