import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildCAppSpec extends JenkinsPipelineSpecification {

    def edgeXBuildCApp = null

    def setup() {

        edgeXBuildCApp = loadPipelineScriptForTest('vars/edgeXBuildCApp.groovy')

        explicitlyMockPipelineVariable('edgex')
        getPipelineMock('edgex.defaultTrue').call(_) >> true
        getPipelineMock('edgex.defaultFalse').call(_) >> false
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BASE_IMAGE': 'MyDockerBaseImage',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)

        when:
            edgeXBuildCApp.prepBaseBuildImage()
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
                'DOCKER_BASE_IMAGE': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)

        when:
            edgeXBuildCApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64', 
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test validate [Should] raise error [When] config does not include a project parameter" () {
        setup:
        when:
            edgeXBuildCApp.validate([:])
        then:
            1 * getPipelineMock('error').call('[edgeXBuildCApp] The parameter "project" is required. This is typically the project name.')
    }

    def "Test toEnvironment [Should] return expected map of default values [When] sandbox environment" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeXBuildCApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'device-sdk-c'
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'sandbox-settings',
                    PROJECT: 'device-sdk-c',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'make test',
                    BUILD_SCRIPT: 'make build',
                    DOCKER_BASE_IMAGE: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest',
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'device-sdk-c',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'pre',
                    BUILD_SNAP: false,
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org',
                    SNYK_DOCKER_SCAN: false
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config" () {
        setup:
        expect:
            edgeXBuildCApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'device-sdk-c',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    dockerFilePath: '/scripts/Dockerfile.alpine-3.9',
                    dockerBuildFilePath: 'Dockerfile.build',
                    dockerBuildContext: '.',
                    dockerBuildArgs: ['MyArg1=Value1', 'MyArg2="Value2"'],
                    dockerNamespace: 'MyDockerNameSpace',
                    dockerImageName: 'MyDockerImageName',
                    dockerNexusRepo: 'MyNexusRepo',
                    semverBump: 'patch'
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'device-sdk-c-settings',
                    PROJECT: 'device-sdk-c',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'MyTestScript',
                    BUILD_SCRIPT: 'MyBuildScript',
                    DOCKER_BASE_IMAGE: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest',
                    DOCKER_FILE_PATH: '/scripts/Dockerfile.alpine-3.9',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'MyDockerImageName',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'patch',
                    BUILD_SNAP: false,
                    DOCKER_BUILD_ARGS: 'MyArg1=Value1,MyArg2="Value2"',
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org',
                    SNYK_DOCKER_SCAN: false
                ]
            ]
    }

}
