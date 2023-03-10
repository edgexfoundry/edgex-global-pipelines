import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildGoParallelSpec extends JenkinsPipelineSpecification {

    def edgeXBuildGoParallel = null

    def setup() {
        edgeXBuildGoParallel = loadPipelineScriptForTest('vars/edgeXBuildGoParallel.groovy')

        explicitlyMockPipelineVariable('edgex')
        getPipelineMock('edgex.defaultTrue').call(null) >> true
        getPipelineMock('edgex.defaultFalse').call(null) >> false
        getPipelineMock('edgex.defaultTrue').call(true) >> true
        getPipelineMock('edgex.defaultFalse').call(true) >> true
        getPipelineMock('edgex.defaultTrue').call(false) >> false
        getPipelineMock('edgex.defaultFalse').call(false) >> false
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'golang:1.18'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine-lts'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call('MyDockerfile') >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
        then:
            1 * getPipelineMock("docker.build").call([
                    'ci-base-image-MyArch',
                    '-f MyDockerfile  --build-arg BASE=edgex-golang-base:1.18-alpine --build-arg http_proxy --build-arg https_proxy --build-arg MAKE="echo noop" --target=MyMockTarget MyDockerBuildContext'])
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist and dockerfile" () {
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call('MyDockerfile') >> false
            getPipelineMock('fileExists').call('go.mod') >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
        then:
            1 * getPipelineMock('sh').call('docker pull edgex-golang-base:1.18-alpine')
            1 * getPipelineMock('sh').call('echo "FROM edgex-golang-base:1.18-alpine\nWORKDIR /edgex\nCOPY go.mod .\nRUN go mod download" | docker build -t ci-base-image-MyArch -f - .')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist and dockerfile" () {
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call(null) >> false
            getPipelineMock('fileExists').call('go.mod') >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
        then:
            1 * getPipelineMock('sh').call('docker pull edgex-golang-base:1.18-alpine')
            1 * getPipelineMock('sh').call('echo "FROM edgex-golang-base:1.18-alpine\nWORKDIR /edgex\nCOPY go.mod .\nRUN go mod download" | docker build -t ci-base-image-MyArch -f - .')
    }

    def "Test prepBaseBuildImage [Should] call docker build with expected arguments [When] non ARM architecture and docker build file does not exist and dockerfile and go.mod does not exist" () {
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call('DoesNotExists') >> false
            getPipelineMock('fileExists').call(null) >> false
            getPipelineMock('fileExists').call('go.mod') >> false
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
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
                'GO_VERSION': '1.18',
                'USE_ALPINE': 'true'
            ]
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('fileExists').call(_) >> true
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.18-alpine'
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64',
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base-arm64:1.18-alpine --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test validate [Should] raise error [When] config does not include a project parameter" () {
        setup:
        when:
            edgeXBuildGoParallel.validate([:])
        then:
            1 * getPipelineMock('error').call('[edgeXBuildGoParallel] The parameter "project" is required. This is typically the project name.')
    }

    def "Test toEnvironment [Should] return expected map of default values [When] sandbox environment" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.20-alpine'
        expect:
            edgeXBuildGoParallel.toEnvironment(config) == expectedResult
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
                    GO_VERSION: '1.20',
                    USE_ALPINE: true,
                    DOCKER_FILE_GLOB: 'cmd/**/Dockerfile',
                    DOCKER_IMAGE_NAME_PREFIX: '',
                    DOCKER_IMAGE_NAME_SUFFIX: '',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_BUILD_IMAGE_TARGET: 'builder',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'pre',
                    //GOPROXY: 'https://nexus3.edgexfoundry.org/repository/go-proxy/',
                    PUBLISH_SWAGGER_DOCS: false,
                    SWAGGER_API_FOLDERS: 'openapi/v1 openapi/v2',
                    // SNAP_CHANNEL: 'latest/edge',
                    BUILD_SNAP: false,
                    BUILD_FAILURE_NOTIFY_LIST: 'edgex-tsc-core@lists.edgexfoundry.org,edgex-tsc-devops@lists.edgexfoundry.org',
                    SNYK_DEBUG: false
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config" () {
        setup:
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'golang:MyGoVersion-alpine'
        expect:
            edgeXBuildGoParallel.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'pSoda',
                    testScript: 'MyTestScript',
                    buildScript: 'MyBuildScript',
                    goVersion: 'MyGoVersion',
                    dockerFileGlobPath: 'MyDockerfileGlobPattern',
                    dockerImageNamePrefix: 'MyPrefix',
                    dockerImageNameSuffix: 'MySuffix',
                    dockerBuildFilePath: 'MyDockerBuildFilePath',
                    dockerBuildContext: 'MyDockerBuildContext',
                    dockerBuildImageTarget: 'MyBuildTarget',
                    dockerNamespace: 'MyDockerNameSpace',
                    dockerImageName: 'MyDockerImageName',
                    dockerNexusRepo: 'MyNexusRepo',
                    semverBump: 'patch',
                    goProxy: 'https://www.example.com/repository/go-proxy/',
                    useAlpineBase: false,
                    publishSwaggerDocs: true,
                    swaggerApiFolders: ['api/v20', 'api/v30'],
                    failureNotify: 'mock@lists.edgexfoundry.org',
                    snykDebug: true
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
                    USE_ALPINE: false,
                    DOCKER_FILE_GLOB: 'MyDockerfileGlobPattern',
                    DOCKER_IMAGE_NAME_PREFIX: 'MyPrefix',
                    DOCKER_IMAGE_NAME_SUFFIX: 'MySuffix',
                    DOCKER_BUILD_FILE_PATH: 'MyDockerBuildFilePath',
                    DOCKER_BUILD_CONTEXT: 'MyDockerBuildContext',
                    DOCKER_BUILD_IMAGE_TARGET: 'MyBuildTarget',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'patch',
                    //GOPROXY: 'https://www.example.com/repository/go-proxy/',
                    PUBLISH_SWAGGER_DOCS: true,
                    SWAGGER_API_FOLDERS: 'api/v20 api/v30',
                    // SNAP_CHANNEL: 'edge',
                    BUILD_SNAP: false,
                    BUILD_FAILURE_NOTIFY_LIST: 'mock@lists.edgexfoundry.org',
                    SNYK_DEBUG: true
                ]
            ]
    }

    def "Test getDockersFromFilesystem [Should] return expected array of docker images and dockerfiles [When] called" () {
        setup:
            explicitlyMockPipelineVariable('out')

            getPipelineMock('sh')([
                script: "for file in `ls cmd/**/Dockerfile`; do echo \"\$(dirname \"\$file\" | cut -d/ -f2),\${file}\"; done",
                returnStdout: true
            ]) >> {
                ['example-1,cmd/example-1/Dockerfile', 'example-2,cmd/example-2/Dockerfile'].join('\n')
            }
        expect:
            edgeXBuildGoParallel.getDockersFromFilesystem(globPattern, '', '') == expectedResult
        where:
            globPattern << [
                'cmd/**/Dockerfile'
            ]
            expectedResult << [
                [
                    [ image: 'example-1', dockerfile: 'cmd/example-1/Dockerfile' ],
                    [ image: 'example-2', dockerfile: 'cmd/example-2/Dockerfile' ]
                ]
            ]
    }

    def "Test getDockersFromFilesystem [Should] return expected array of docker images and dockerfiles [When] called with custom prefix and suffix" () {
        setup:
            explicitlyMockPipelineVariable('out')

            getPipelineMock('sh')([
                script: 'for file in `ls cmd/**/Dockerfile`; do echo "$(dirname "$file" | cut -d/ -f2),${file}"; done',
                returnStdout: true
            ]) >> {
                ['example-1,cmd/example-1/Dockerfile', 'example-2,cmd/example-2/Dockerfile'].join('\n')
            }
        expect:
            edgeXBuildGoParallel.getDockersFromFilesystem(globPattern, 'edgex-', '-suffix') == expectedResult
        where:
            globPattern << [
                'cmd/**/Dockerfile'
            ]
            expectedResult << [
                [
                    [ image: 'edgex-example-1-suffix', dockerfile: 'cmd/example-1/Dockerfile' ],
                    [ image: 'edgex-example-2-suffix', dockerfile: 'cmd/example-2/Dockerfile' ]
                ]
            ]
    }
}