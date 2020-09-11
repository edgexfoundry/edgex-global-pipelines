import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class edgeXReleaseGitHubAssetsSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitHubAssets = null
    def validReleaseInfo

    def setup() {
        edgeXReleaseGitHubAssets = loadPipelineScriptForTest('vars/edgeXReleaseGitHubAssets.groovy')
        explicitlyMockPipelineVariable('out')
        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'repo': 'https://github.com/edgexfoundry/sample-service.git',
            'releaseStream': 'master',
            'assets': [
                'https://nexus-location/asset1',
                'https://nexus-location/asset2',
                'https://nexus-location/asset3'
            ],
            'gitHubRelease': true
        ]
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a name attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'name'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'name\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'version'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'version\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a assets attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'assets'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'assets\'')
    }

    def "Test createGitHubRelease [Should] create assets folder and download remote assets [When] called" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo, 'credentials')
        then:
            1 * getPipelineMock('sh').call('mkdir assets')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset1 -P assets/')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset2 -P assets/')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset3 -P assets/')
    }

    def "Test createGitHubRelease [Should] call create-github-release [When] called" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo, 'credentials')
        then:
            1 * getPipelineMock('sh').call("create-github-release --repo 'edgexfoundry/sample-service' --tag 'v1.2.3' --assets 'assets/'")
    }

}