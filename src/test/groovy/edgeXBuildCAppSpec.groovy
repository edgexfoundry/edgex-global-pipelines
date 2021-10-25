import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildCAppSpec extends JenkinsPipelineSpecification {

    def edgeXBuildCApp = null

    def setup() {

        edgeXBuildCApp = loadPipelineScriptForTest('vars/edgeXBuildCApp.groovy')

        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable("out")
        getPipelineMock('edgex.defaultTrue').call(_) >> true
        getPipelineMock('edgex.defaultFalse').call(_) >> false
    }

    def "Test prepBaseBuildImage [Should] call tag proper repo build image [When] LTS is true and forceBuild is false" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'PROJECT': 'MyProject'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('edgex.isLTS').call() >> true

            explicitlyMockPipelineVariable('edgeXLTS')
            getPipelineMock('edgeXLTS.getLatestLTSCommitId').call() >> 'acbc8cf'

        when:
            edgeXBuildCApp.prepBaseBuildImage(false)
        then:
            1 * getPipelineMock('sh').call('docker pull nexus3.edgexfoundry.org:10002/MyProject-builder-MyArch:acbc8cf')
            1 * getPipelineMock('sh').call('docker tag nexus3.edgexfoundry.org:10002/MyProject-builder-MyArch:acbc8cf ci-base-image-MyArch')
    }

    // this is the use case when we have an LTS release and we are building the repo builder images
    def "Test prepBaseBuildImage [Should] call generateBuildImage [When] LTS is true and forceBuild is true" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'PROJECT': 'MyProject',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.isLTS').call() >> true
            getPipelineMock('edgex.getCBaseImage').call() >> 'MyDockerRegistry/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.prepBaseBuildImage(true)
        then:
            1 * getPipelineMock('sh').call('echo 0.0.0 > ./VERSION')
            1 * getPipelineMock('docker.build').call([
                    'MyProject-builder-MyArch',
                    '-f MyDockerBuildFilePath  --build-arg BASE=MyDockerRegistry/edgex-gcc-base:latest MyDockerBuildContext'])
    }

    // this is the typical use case in a non-lts build
    def "Test prepBaseBuildImage [Should] call generateBuildImage [When] LTS is false and forceBuild not passed in" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'PROJECT': 'MyProject',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.isLTS').call() >> false
            getPipelineMock('edgex.getCBaseImage').call() >> 'MyDockerRegistry/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-MyArch',
                    '-f MyDockerBuildFilePath  --build-arg BASE=MyDockerRegistry/edgex-gcc-base:latest MyDockerBuildContext'])
    }

    def "Test generateBuildImage [Should] call docker build with expected arguments [When] non ARM architecture" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getCBaseImage').call() >> 'MyDockerRegistry/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.generateBuildImage('ci-base-image-MyArch')
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-MyArch',
                    '-f MyDockerBuildFilePath  --build-arg BASE=MyDockerRegistry/edgex-gcc-base:latest --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test generateBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getCBaseImage').call() >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.generateBuildImage('ci-base-image-arm64')
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64',
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test generateBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry and docker build file does not exist" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_FILE_PATH': 'MyDockerfile',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyBuildTarget'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call('MyDockerfile') >> true
            getPipelineMock('edgex.getCBaseImage').call() >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.generateBuildImage('ci-base-image-arm64')
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64',
                    '-f MyDockerfile  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest --build-arg http_proxy --build-arg https_proxy --build-arg MAKE="echo noop" --target=MyBuildTarget MyDockerBuildContext'])
    }

    def "Test generateBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry and docker build file does not exist and dockerfile does not exist" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_FILE_PATH': 'MyDockerfile',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyBuildTarget'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call('MyDockerfile') >> false
            getPipelineMock('edgex.getCBaseImage').call() >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.generateBuildImage('ci-base-image-arm64')
        then:
            1 * getPipelineMock('sh').call('docker pull nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest')
            1 * getPipelineMock('sh').call('docker tag nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest ci-base-image-arm64')
    }

    def "Test generateBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry and docker build file does not exist and dockerfile does is null" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyBuildTarget'
            ]
            edgeXBuildCApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call(null) >> false
            getPipelineMock('edgex.getCBaseImage').call() >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest'

        when:
            edgeXBuildCApp.generateBuildImage('ci-base-image-arm64')
        then:
            1 * getPipelineMock('sh').call('docker pull nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest')
            1 * getPipelineMock('sh').call('docker tag nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base-arm64:latest ci-base-image-arm64')
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
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_BUILD_IMAGE_TARGET: 'builder',
                    DOCKER_IMAGE_NAME: 'device-sdk',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'pre',
                    BUILD_SNAP: false,
                    BUILD_FAILURE_NOTIFY_LIST: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org'
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
                    dockerBuildImageTarget: 'MyBuildTarget',
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
                    DOCKER_FILE_PATH: '/scripts/Dockerfile.alpine-3.9',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'MyDockerImageName',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    DOCKER_BUILD_IMAGE_TARGET: 'MyBuildTarget',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'patch',
                    BUILD_SNAP: false,
                    DOCKER_BUILD_ARGS: 'MyArg1=Value1,MyArg2="Value2"',
                    BUILD_FAILURE_NOTIFY_LIST: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org'
                ]
            ]
    }

}
