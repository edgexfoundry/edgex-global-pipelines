import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseGitHubAssetsSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitHubAssets = null
    def validReleaseInfo

    def setup() {
        edgeXReleaseGitHubAssets = loadPipelineScriptForTest('vars/edgeXReleaseGitHubAssets.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'repo': 'https://github.com/edgexfoundry/sample-service.git',
            'releaseStream': 'master',
            'gitHubReleaseAssets': [
                'https://nexus-location/asset1',
                'https://nexus-location/asset2',
                'https://nexus-location/asset3'
            ],
            'gitHubRelease': true
        ]
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a repo attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'repo'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'repo\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'version'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'version\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a gitHubReleaseAssets attribute" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.validate(validReleaseInfo.findAll {it.key != 'gitHubReleaseAssets'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitHubAssets]: Release yaml does not contain \'gitHubReleaseAssets\'')
    }

    def "Test getRepoInfo [Should] raise error [When] repo is malformed" () {
        setup:
        when:
            edgeXReleaseGitHubAssets.getRepoInfo('malformed repo')
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseGitHubAssets]: Release yaml 'repo' value is malformed")
    }

    def "Test getRepoInfo [Should] return expected [When] repo is formed correctly" () {
        setup:
        expect:
            result == edgeXReleaseGitHubAssets.getRepoInfo(repo)
        where:
            repo << [
                'https://github.com/edgexfoundry/sample-service.git',
                'https://github.com/soda480/test-repo1.git'
            ]
            result << [
                [ORG:'edgexfoundry', REPO:'sample-service'],
                [ORG:'soda480', REPO:'test-repo1']
            ]
    }

    def "Test getCredentialsId [Should] return expected [When] called in production" () {
        setup:
            def environmentVariables = [
                'SILO': 'production'
            ]
            edgeXReleaseGitHubAssets.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeXReleaseGitHubAssets.getCredentialsId() == 'edgex-jenkins-github-personal-access-token'
    }

    def "Test getCredentialsId [Should] return expected [When] called in non-production" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXReleaseGitHubAssets.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeXReleaseGitHubAssets.getCredentialsId() == 'edgex-jenkins-access-username'
    }

    def "Test createGitHubRelease [Should] create assets folder and download remote assets [When] called" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo)
        then:
            1 * getPipelineMock('sh').call('mkdir assets')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset1 -P assets/')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset2 -P assets/')
            1 * getPipelineMock('sh').call('wget https://nexus-location/asset3 -P assets/')
    }

    def "Test createGitHubRelease [Should] call create-github-release [When] DRY_RUN is false in non production" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo)
        then:
            1 * getPipelineMock('sh').call("create-github-release --repo 'edgexfoundry/sample-service' --tag 'v1.2.3' --assets 'assets/'")
            1 * getPipelineMock('usernamePassword.call')(['credentialsId':'edgex-jenkins-access-username', 'usernameVariable':'GH_TOKEN_USR', 'passwordVariable':'GH_TOKEN_PSW'])
    }

    def "Test createGitHubRelease [Should] call create-github-release [When] DRY_RUN is false in production" () {
        setup:
            def environmentVariables = [
                'SILO': 'production'
            ]
            edgeXReleaseGitHubAssets.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo)
        then:
            1 * getPipelineMock('sh').call("create-github-release --repo 'edgexfoundry/sample-service' --tag 'v1.2.3' --assets 'assets/'")
            1 * getPipelineMock('usernamePassword.call')(['credentialsId':'edgex-jenkins-github-personal-access-token', 'usernameVariable':'GH_TOKEN_USR', 'passwordVariable':'GH_TOKEN_PSW'])
    }

    def "Test createGitHubRelease [Should] call create-github-release [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-devops/github-release:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseGitHubAssets.createGitHubRelease(validReleaseInfo)
        then:
            0 * getPipelineMock('sh').call("create-github-release --repo 'edgexfoundry/sample-service' --tag 'v1.2.3' --assets 'assets/'")
    }
}