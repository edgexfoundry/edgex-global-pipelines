import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildGoParallelSpec extends JenkinsPipelineSpecification {

    def edgeXBuildGoParallel = null
    def edgex = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXBuildGoParallel = loadPipelineScriptForTest('vars/edgeXBuildGoParallel.groovy')
        edgex = loadPipelineScriptForTest('vars/edgex.groovy')
        edgeXBuildGoParallel.getBinding().setVariable('edgex', edgex)

        explicitlyMockPipelineStep('echo')
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
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
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
                'DOCKER_BASE_IMAGE': 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
                'DOCKER_BUILD_FILE_PATH': 'MyDockerBuildFilePath',
                'DOCKER_BUILD_CONTEXT': 'MyDockerBuildContext'
            ]
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXBuildGoParallel.prepBaseBuildImage()
        then:
            1 * getPipelineMock('docker.build').call([
                    'ci-base-image-arm64', 
                    '-f MyDockerBuildFilePath  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base-arm64:1.13-alpine --build-arg http_proxy --build-arg https_proxy MyDockerBuildContext'])
    }

    def "Test validate [Should] raise error [When] config does not include a project parameter" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXBuildGoParallel.validate([:])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test toEnvironment [Should] return expected map of default values [When] sandbox environment" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXBuildGoParallel.getBinding().setVariable('env', environmentVariables)
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
                    GO_VERSION: '1.13',
                    DOCKER_BASE_IMAGE: 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
                    DOCKER_FILE_GLOB: 'cmd/**/Dockerfile',
                    DOCKER_IMAGE_NAME_PREFIX: 'docker-',
                    DOCKER_IMAGE_NAME_SUFFIX: '-go',
                    DOCKER_BUILD_FILE_PATH: 'Dockerfile.build',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'pre',
                    GOPROXY: 'https://nexus3.edgexfoundry.org/repository/go-proxy/',
                    PUBLISH_SWAGGER_DOCS: false,
                    SWAGGER_API_FOLDERS: 'openapi/v1 openapi/v2'
                    /*SNAP_CHANNEL: 'latest/edge',
                    BUILD_SNAP: false,
                    */
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected map of overriden values [When] non-sandbox environment and custom config" () {
        setup:
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
                    dockerNamespace: 'MyDockerNameSpace',
                    dockerImageName: 'MyDockerImageName',
                    dockerNexusRepo: 'MyNexusRepo',
                    semverBump: 'patch',
                    goProxy: 'https://www.example.com/repository/go-proxy/',
                    useAlpineBase: true,
                    publishSwaggerDocs: true,
                    swaggerApiFolders: ['api/v20', 'api/v30']
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
                    DOCKER_BASE_IMAGE: 'golang:MyGoVersion-alpine',
                    DOCKER_FILE_GLOB: 'MyDockerfileGlobPattern',
                    DOCKER_IMAGE_NAME_PREFIX: 'MyPrefix',
                    DOCKER_IMAGE_NAME_SUFFIX: 'MySuffix',
                    DOCKER_BUILD_FILE_PATH: 'MyDockerBuildFilePath',
                    DOCKER_BUILD_CONTEXT: 'MyDockerBuildContext',
                    DOCKER_REGISTRY_NAMESPACE: 'MyDockerNameSpace',
                    DOCKER_NEXUS_REPO: 'MyNexusRepo',
                    BUILD_DOCKER_IMAGE: true,
                    PUSH_DOCKER_IMAGE: true,
                    SEMVER_BUMP_LEVEL: 'patch',
                    GOPROXY: 'https://www.example.com/repository/go-proxy/',
                    PUBLISH_SWAGGER_DOCS: true,
                    SWAGGER_API_FOLDERS: 'api/v20 api/v30'
                    /*SNAP_CHANNEL: 'edge',
                    BUILD_SNAP: false,
                    */
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
            edgeXBuildGoParallel.getDockersFromFilesystem(globPattern, 'docker-', '-go') == expectedResult
        where:
            globPattern << [
                'cmd/**/Dockerfile'
            ]
            expectedResult << [
                [
                    [ image: 'docker-example-1-go', dockerfile: 'cmd/example-1/Dockerfile' ],
                    [ image: 'docker-example-2-go', dockerfile: 'cmd/example-2/Dockerfile' ]
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