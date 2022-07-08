import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildGoAppSpec extends JenkinsPipelineSpecification {

    def edgeXBuildGoApp = null

    def setup() {
        edgeXBuildGoApp = loadPipelineScriptForTest('vars/edgeXBuildGoApp.groovy')

        explicitlyMockPipelineVariable('edgex')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] LTS is false and non ARM architecture and alpine base" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock("docker.build").call([
                   'ci-base-image-MyArch',
                   '-f MyDockerBuildFilePath  --build-arg BASE=edgex-golang-base:1.18-alpine --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] LTS is false and non ARM architecture and non-alpine base" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'false'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'golang:1.18'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock("docker.build").call([
                   'ci-base-image-MyArch',
                   '-f MyDockerBuildFilePath  --build-arg BASE=golang:1.18 MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] LTS is true and non ARM architecture and alpine base" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine-lts'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock("docker.build").call([
                   'ci-base-image-MyArch',
                   '-f MyDockerBuildFilePath  --build-arg BASE=edgex-golang-base:1.18-alpine-lts MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_FILE_PATH': 'MyDockerfile',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyMockTarget',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call('MyDockerfile') >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock("docker.build").call([
                   'ci-base-image-MyArch',
                   '-f MyDockerfile  --build-arg BASE=edgex-golang-base:1.18-alpine --build-arg http_proxy --build-arg https_proxy --build-arg MAKE="echo noop" --target=MyMockTarget MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist and dockerfile does not exist" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_FILE_PATH': 'MyDockerfile',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyMockTarget',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call(null) >> false
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('sh').call('docker pull edgex-golang-base:1.18-alpine')
            1 * getPipelineMock('sh').call('docker tag edgex-golang-base:1.18-alpine ci-base-image-MyArch')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist and dockerfile is null" () {
        setup:
            def environmentVariables = [
                'ARCH': 'MyArch',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'DoesNotExists',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'DOCKER_BUILD_IMAGE_TARGET': 'MyMockTarget',
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call(null) >> false
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('sh').call('docker pull edgex-golang-base:1.18-alpine')
            1 * getPipelineMock('sh').call('docker tag edgex-golang-base:1.18-alpine ci-base-image-MyArch')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] ARM architecture and base image contains registry" () {
        setup:
            def environmentVariables = [
                'ARCH': 'arm64',
                'DOCKER_REGISTRY': 'nexus3.edgexfoundry.org',
                'http_proxy': 'MyHttpProxy',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext',
                'GO_VERSION': '1.12',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine'
        when:
            edgeXBuildGoApp.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64',
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base-arm64:1.12.14-alpine --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test validate [Should] raise error [When] config does not include a project parameter" () {
        setup:
        when:
            edgeXBuildGoApp.validate([:])
        then:
            1 * getPipelineMock('error').call('[edgeXBuildGoApp] The parameter "project" is required. This is typically the project name.')
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config and artifact is a docker image" () {
        setup:
            getPipelineMock('edgex.defaultTrue').call(null) >> true
            getPipelineMock('edgex.defaultTrue').call(true) >> true
            getPipelineMock('edgex.defaultTrue').call(false) >> false
            getPipelineMock('edgex.defaultFalse').call(null) >> false
            getPipelineMock('edgex.defaultFalse').call(true) >> true
            getPipelineMock('edgex.defaultFalse').call(false) >> false
        expect:
            edgeXBuildGoApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda-go',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    goVersion: 'MyGoVersion',
                    dockerFilePath: 'MyDockerFilePath',
                    dockerBuildFilePath: 'MyDockerBuildFilePath',
                    dockerBuildContext: 'MyDockerBuildContext',
                    dockerBuildImageTarget: 'MyBuildTarget',
                    dockerBuildArgs: ['MyArg1=Value1', 'MyArg2="Value2"'],
                    dockerNamespace: 'MyDockerNameSpace',
                    dockerImageName: 'MyDockerImageName',
                    dockerNexusRepo: 'MyNexusRepo',
                    buildExperimentalDockerImage: true,
                    semverBump: 'patch',
                    goProxy: 'https://www.example.com/repository/go-proxy/',
                    useAlpineBase: false,
                    // snapChannel: 'edge',
                    publishSwaggerDocs: true,
                    swaggerApiFolders: ['api/v20', 'api/v30'],
                    artifactTypes: ['docker'],
                    failureNotify: 'mock@lists.edgexfoundry.org',
                    snykDebug: true
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'pSoda-go-settings',
                    PROJECT: 'pSoda-go',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'MyTestScript',
                    BUILD_SCRIPT: 'MyBuildScript',
                    GO_VERSION: 'MyGoVersion',,
                    USE_ALPINE: false,
                    //GOPROXY: 'https://www.example.com/repository/go-proxy/',
                    DOCKER_FILE_PATH: 'MyDockerFilePath',
                    DOCKER_BUILD_FILE_PATH: 'MyDockerBuildFilePath',
                    DOCKER_BUILD_CONTEXT: 'MyDockerBuildContext',
                    DOCKER_BUILD_IMAGE_TARGET: 'MyBuildTarget',
                    DOCKER_IMAGE_NAME: 'MyDockerImageName',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    BUILD_EXPERIMENTAL_DOCKER_IMAGE: true,
                    BUILD_STABLE_DOCKER_IMAGE: false,
                    SEMVER_BUMP_LEVEL: 'patch',
                    // SNAP_CHANNEL: 'edge',
                    BUILD_SNAP: false,
                    PUBLISH_SWAGGER_DOCS: true,
                    SWAGGER_API_FOLDERS: 'api/v20 api/v30',
                    DOCKER_BUILD_ARGS: 'MyArg1=Value1,MyArg2="Value2"',
                    ARTIFACT_ROOT: "archives/bin",
                    ARTIFACT_TYPES: 'docker',
                    SHOULD_BUILD: true,
                    BUILD_FAILURE_NOTIFY_LIST: 'mock@lists.edgexfoundry.org',
                    SNYK_DEBUG: true
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config and artifact is an archive" () {
        setup:
            getPipelineMock('edgex.defaultTrue').call(null) >> true
            getPipelineMock('edgex.defaultTrue').call(true) >> true
            getPipelineMock('edgex.defaultTrue').call(false) >> false
            getPipelineMock('edgex.defaultFalse').call(null) >> false
            getPipelineMock('edgex.defaultFalse').call(true) >> true
            getPipelineMock('edgex.defaultFalse').call(false) >> false
        expect:
            edgeXBuildGoApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    goVersion: 'MyGoVersion',
                    buildImage: false,
                    semverBump: 'pLevel',
                    goProxy: 'https://www.example.com/repository/go-proxy/',
                    artifactTypes: ['archive']
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'pSoda-settings',
                    PROJECT: 'pSoda',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'MyTestScript',
                    BUILD_SCRIPT: 'MyBuildScript',
                    GO_VERSION: 'MyGoVersion',
                    USE_ALPINE: true,
                    //GOPROXY: 'https://www.example.com/repository/go-proxy/',
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_BUILD_IMAGE_TARGET: 'builder',
                    DOCKER_IMAGE_NAME: 'pSoda',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: false,
                    PUSH_DOCKER_IMAGE: false,
                    BUILD_EXPERIMENTAL_DOCKER_IMAGE: false,
                    BUILD_STABLE_DOCKER_IMAGE: false,
                    SEMVER_BUMP_LEVEL: 'pLevel',
                    BUILD_SNAP: false,
                    PUBLISH_SWAGGER_DOCS: false,
                    SWAGGER_API_FOLDERS: 'openapi/v1',
                    ARTIFACT_ROOT: 'archives/bin',
                    ARTIFACT_TYPES: 'archive',
                    SHOULD_BUILD: true,
                    ARCHIVE_ARTIFACTS: '**/*.tar.gz **/*.zip',
                    BUILD_FAILURE_NOTIFY_LIST: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
                    SNYK_DEBUG: false
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config and artifact unknown" () {
        setup:
            getPipelineMock('edgex.defaultTrue').call(null) >> true
            getPipelineMock('edgex.defaultTrue').call(true) >> true
            getPipelineMock('edgex.defaultTrue').call(false) >> false
            getPipelineMock('edgex.defaultFalse').call(null) >> false
            getPipelineMock('edgex.defaultFalse').call(true) >> true
            getPipelineMock('edgex.defaultFalse').call(false) >> false
        expect:
            edgeXBuildGoApp.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    goVersion: 'MyGoVersion',
                    semverBump: 'pLevel',
                    goProxy: 'https://www.example.com/repository/go-proxy/',
                    artifactTypes: ['foo']
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'pSoda-settings',
                    PROJECT: 'pSoda',
                    USE_SEMVER: true,
                    TEST_SCRIPT: 'MyTestScript',
                    BUILD_SCRIPT: 'MyBuildScript',
                    GO_VERSION: 'MyGoVersion',
                    USE_ALPINE: true,
                    //GOPROXY: 'https://www.example.com/repository/go-proxy/',
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_BUILD_IMAGE_TARGET: 'builder',
                    DOCKER_IMAGE_NAME: 'pSoda',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: false,
                    PUSH_DOCKER_IMAGE: false,
                    BUILD_EXPERIMENTAL_DOCKER_IMAGE: false,
                    BUILD_STABLE_DOCKER_IMAGE: false,
                    SEMVER_BUMP_LEVEL: 'pLevel',
                    BUILD_SNAP: false,
                    PUBLISH_SWAGGER_DOCS: false,
                    SWAGGER_API_FOLDERS: 'openapi/v1',
                    ARTIFACT_ROOT: 'archives/bin',
                    ARTIFACT_TYPES: 'foo',
                    SHOULD_BUILD: false,
                    BUILD_FAILURE_NOTIFY_LIST: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
                    SNYK_DEBUG: false
                ]
            ]
    }

    def "Test buildArtifact [Should] return expected build the correct artifact [When] building x86_64 docker a docker image" () {
        setup:
            def environmentVariables = [
                'ARTIFACT_TYPES': 'docker',
                'DOCKER_IMAGE_NAME': 'mock-image',
                'ARCH': 'x86_64'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('edgeXDocker')
            explicitlyMockPipelineStep('edgeXDocker.build')
        when:
            edgeXBuildGoApp.buildArtifact()
        then:
            1 * getPipelineMock('edgeXDocker.build').call('mock-image', "ci-base-image-x86_64")
    }

    def "Test buildArtifact [Should] return expected build the correct artifact [When] building arm64 docker a docker image" () {
        setup:
            def environmentVariables = [
                'ARTIFACT_TYPES': 'docker',
                'DOCKER_IMAGE_NAME': 'mock-image',
                'ARCH': 'arm64'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('edgeXDocker')
            explicitlyMockPipelineStep('edgeXDocker.build')
        when:
            edgeXBuildGoApp.buildArtifact()
        then:
            1 * getPipelineMock('edgeXDocker.build').call('mock-image-arm64', "ci-base-image-arm64")
    }

    def "Test buildArtifact [Should] return expected build the correct artifact [When] building generic x86_64 archive" () {
        setup:
            def environmentVariables = [
                'ARTIFACT_TYPES': 'archive',
                'ARCH': 'x86_64',
                'BUILD_SCRIPT': 'mock build-script',
                'ARTIFACT_ROOT': 'archives/bin'
            ]
            edgeXBuildGoApp.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('docker.image')('ci-base-image-x86_64') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXBuildGoApp.buildArtifact()
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "-u 0:0" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('mock build-script')
            1 * getPipelineMock('sh').call('chown -R 1001:1001 ${ARTIFACT_ROOT}/..')
            1 * getPipelineMock("stash").call([name: 'artifacts-x86_64', includes: 'archives/bin/**', useDefaultExcludes:false, allowEmpty:true]) >> true
    }
}