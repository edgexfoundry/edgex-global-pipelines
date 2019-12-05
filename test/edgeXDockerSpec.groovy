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
            edgeXDocker.getBinding().setVariable('GIT_COMMIT', 'MyGitCommit')
            edgeXDocker.getBinding().setVariable('ARCH', 'MyArch')
            edgeXDocker.getBinding().setVariable('DOCKER_FILE_PATH', 'MyDockerFilePath')
            edgeXDocker.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')
            explicitlyMockPipelineVariable('docker')
        when:
            edgeXDocker.build('MyDockerImageName')
        then:
            1 * getPipelineMock("docker.build").call(['MyDockerImageName', "-f MyDockerFilePath   --label 'git_sha=MyGitCommit' --label 'arch=MyArch' MyDockerBuildContext"])
    }

    def "Test build [Should] call docker build with expected arguments [When] BUILD_SCRIPT DOCKER_BUILD_ARGS" () {
        setup:
            def environmentVariables = [
                'BUILD_SCRIPT': 'MyBuildScript',
                'DOCKER_BUILD_ARGS': 'MyArg1,MyArg2,MyArg3',
                'http_proxy': 'MyHttpProxy',
                'VERSION': 'MyVersion'
            ]
            edgeXDocker.getBinding().setVariable('env', environmentVariables)
            edgeXDocker.getBinding().setVariable('GIT_COMMIT', 'MyGitCommit')
            edgeXDocker.getBinding().setVariable('ARCH', 'MyArch')
            edgeXDocker.getBinding().setVariable('DOCKER_FILE_PATH', 'MyDockerFilePath')
            edgeXDocker.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')
            edgeXDocker.getBinding().setVariable('BUILD_SCRIPT', 'MyBuildScript')
            edgeXDocker.getBinding().setVariable('VERSION', 'MyVersion')

            explicitlyMockPipelineVariable('docker')
        when:
            edgeXDocker.build('MyDockerImageName')
        then:
            1 * getPipelineMock("docker.build").call(['MyDockerImageName', "-f MyDockerFilePath  --build-arg MAKE='MyBuildScript' --build-arg http_proxy --build-arg https_proxy --build-arg MyArg1 --build-arg MyArg2 --build-arg MyArg3  --label 'git_sha=MyGitCommit' --label 'arch=MyArch' --label 'version=MyVersion' MyDockerBuildContext"])
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
            explicitlyMockPipelineVariable('docker')
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
            explicitlyMockPipelineVariable('docker')
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
            explicitlyMockPipelineVariable('docker')
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
            explicitlyMockPipelineVariable('docker')
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

}
