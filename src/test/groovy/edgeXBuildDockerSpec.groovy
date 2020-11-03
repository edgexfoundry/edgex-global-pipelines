import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildDockerSpec extends JenkinsPipelineSpecification {

    def edgeXBuildDocker = null

    def setup() {
        edgeXBuildDocker = loadPipelineScriptForTest('vars/edgeXBuildDocker.groovy')

        explicitlyMockPipelineVariable('edgex')
    }

    def "Test validate [Should] raise error [When] config has no project" () {
        setup:
        when:
            edgeXBuildDocker.validate([:])
        then:
            1 * getPipelineMock('error').call('[edgeXBuildDocker] The parameter "project" is required. This is typically the project name')
    }

    def "Test toEnvironment [Should] return expected map [When] called" () {
        setup:
            getPipelineMock('edgex.defaultTrue').call(true) >> true
            getPipelineMock('edgex.defaultTrue').call(false) >> false
            getPipelineMock('edgex.defaultTrue').call(null) >> true
            getPipelineMock('edgex.defaultFalse').call(_) >> false
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
                    SEMVER_BUMP_LEVEL: 'patch',
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org'
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
                    SEMVER_BUMP_LEVEL: 'pre',
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org'
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
                    RELEASE_BRANCH_OVERRIDE: 'golang',
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org'
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
                    RELEASE_BRANCH_OVERRIDE: 'golang',
                    SECURITY_NOTIFY_LIST: 'security-issues@lists.edgexfoundry.org'
                ]
            ]
    }

}
