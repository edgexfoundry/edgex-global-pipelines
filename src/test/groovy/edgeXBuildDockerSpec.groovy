import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildDockerSpec extends JenkinsPipelineSpecification {

    def edgeXBuildDocker = null
    def edgex = null
    def environment = [:]

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXBuildDocker = loadPipelineScriptForTest('vars/edgeXBuildDocker.groovy')
        edgex = loadPipelineScriptForTest('vars/edgex.groovy')
        edgeXBuildDocker.getBinding().setVariable('env', environment)
        edgeXBuildDocker.getBinding().setVariable('edgex', edgex)
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('echo')
    }

    def "Test validate [Should] raise error [When] config has no project" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXBuildDocker.validate([:])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test toEnvironment [Should] return expected map [When] called" () {
        setup:

        expect:
            edgeXBuildDocker.toEnvironment(config) == expectedResult
        where:
            config << [
                [
                    project: 'MyProject',
                    dockerNexusRepo: 'MyDockerNexusRepo',
                    semverBump: 'patch'
                ], [
                    project: 'MyProject',
                    dockerTags: ['MyTag1', 'MyTag2'],
                    dockerBuildArgs: ['MyArg1', 'MyArg2']

                ], [
                    project: 'MyProject',
                    releaseBranchOverride: 'golang'
                ], [
                    project: 'MyProject',
                    releaseBranchOverride: 'golang',
                    dockerPushLatest: false
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'docker-MyProject',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'MyDockerNexusRepo',
                    DOCKER_PUSH_LATEST: true,
                    PUSH_DOCKER_IMAGE: true,
                    ARCHIVE_IMAGE: false,
                    ARCHIVE_NAME: 'MyProject-archive.tar.gz',
                    SEMVER_BUMP_LEVEL: 'patch'
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'docker-MyProject',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    DOCKER_PUSH_LATEST: true,
                    PUSH_DOCKER_IMAGE: true,
                    ARCHIVE_IMAGE: false,
                    ARCHIVE_NAME: 'MyProject-archive.tar.gz',
                    DOCKER_CUSTOM_TAGS: 'MyTag1 MyTag2',
                    DOCKER_BUILD_ARGS: 'MyArg1,MyArg2',
                    SEMVER_BUMP_LEVEL: 'pre'
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'docker-MyProject',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    DOCKER_PUSH_LATEST: true,
                    PUSH_DOCKER_IMAGE: true,
                    ARCHIVE_IMAGE: false,
                    ARCHIVE_NAME: 'MyProject-archive.tar.gz',
                    SEMVER_BUMP_LEVEL: 'pre',
                    RELEASE_BRANCH_OVERRIDE: 'golang'
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                    DOCKER_FILE_PATH: 'Dockerfile',
                    DOCKER_BUILD_CONTEXT: '.',
                    DOCKER_IMAGE_NAME: 'docker-MyProject',
                    DOCKER_REGISTRY_NAMESPACE: '',
                    DOCKER_NEXUS_REPO: 'staging',
                    DOCKER_PUSH_LATEST: false,
                    PUSH_DOCKER_IMAGE: true,
                    ARCHIVE_IMAGE: false,
                    ARCHIVE_NAME: 'MyProject-archive.tar.gz',
                    SEMVER_BUMP_LEVEL: 'pre',
                    RELEASE_BRANCH_OVERRIDE: 'golang'
                ]
            ]
    }

}
