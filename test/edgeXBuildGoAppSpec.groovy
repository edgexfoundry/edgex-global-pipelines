import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildGoAppSpec extends JenkinsPipelineSpecification {

    def edgeXBuildGoApp = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {

        edgeXBuildGoApp = loadPipelineScriptForTest('vars/edgeXBuildGoApp.groovy')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BASE_IMAGE': 'MyDockerBaseImage'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('docker')
            edgeXBuildGoApp.getBinding().setVariable('ARCH', 'MyArch')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BASE_IMAGE', 'MyDockerBaseImage')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BUILD_FILE_PATH', 'MyDockerBuildFilePath')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
             1 * getPipelineMock("docker.build").call([
                    'ci-base-image-MyArch',
                    '-f MyDockerBuildFilePath  --build-arg BASE=MyDockerBaseImage --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BASE_IMAGE': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('docker')
            edgeXBuildGoApp.getBinding().setVariable('ARCH', 'arm64')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BASE_IMAGE', 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BUILD_FILE_PATH', 'MyDockerBuildFilePath')
            edgeXBuildGoApp.getBinding().setVariable('DOCKER_BUILD_CONTEXT', 'MyDockerBuildContext')
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64', 
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base-arm64:1.12.14-alpine --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test validate [Should] raise error [When] config does not include a project parameter" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXBuildGoApp.validate([:])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version" () {
        setup:
        expect:
            edgeXBuildGoApp.getGoLangBaseImage(version) == expectedResult
        where:
            version << [
                '1.11',
                '1.12',
                '1.13',
                '1.01'
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.11.13-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
                'golang:1.01-alpine'
            ]
    }

    def "Test toEnvironment [Should] return expected map of default values [When] sandbox environment" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('edgex.defaultTrue')(null) >> {
                true
            }
        expect:
            edgeXBuildGoApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda'
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'sandbox-settings',
                    PROJECT: 'pSoda',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'make test',
                    BUILD_SCRIPT: 'make build',
                    GO_VERSION: '1.12',
                    DOCKER_BASE_IMAGE: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine',
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'docker-pSoda',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'pre'
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config" () {
        setup:
            getPipelineMock('edgex.defaultTrue')(null) >> {
                false
            }
        expect:
            edgeXBuildGoApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    goVersion: 'MyGoVersion',
                    dockerFilePath: 'MyDockerFilePath',
                    dockerBuildFilePath: 'MyDockerBuildFilePath',
                    dockerBuildContext: 'MyDockerBuildContext',
                    dockerNamespace: 'MyDockerNameSpace',
                    dockerImageName: 'MyDockerImageName',
                    dockerNexusRepo: 'MyNexusRepo',
                    semverBump: 'patch'
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'pSoda-settings',
                    PROJECT: 'pSoda',
                    USE_SEMVER: false,
                    TEST_SCRIPT: 'MyTestScript',
                    BUILD_SCRIPT: 'MyBuildScript',
                    GO_VERSION: 'MyGoVersion',
                    DOCKER_BASE_IMAGE: 'golang:MyGoVersion-alpine',
                    DOCKER_FILE_PATH: 'MyDockerFilePath',
                    DOCKER_BUILD_FILE_PATH: 'MyDockerBuildFilePath',
                    DOCKER_BUILD_CONTEXT: 'MyDockerBuildContext',
                    DOCKER_IMAGE_NAME: 'MyDockerImageName',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    BUILD_DOCKER_IMAGE: false,
                    PUSH_DOCKER_IMAGE: false,
                    SEMVER_BUMP_LEVEL: 'patch'
                ]
            ]
    }

}
