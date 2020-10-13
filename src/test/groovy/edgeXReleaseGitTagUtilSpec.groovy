import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseGitTagUtilSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitTagUtil = null
    def validReleaseInfo

    def setup() {
        edgeXReleaseGitTagUtil = loadPipelineScriptForTest('vars/edgeXReleaseGitTagUtil.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXInfraLFToolsSign')
        explicitlyMockPipelineVariable('edgeXReleaseGitTag')

        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'releaseStream': 'master',
            'repo': 'https://github.com/edgexfoundry/sample-service.git'
        ]
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a name attribute" () {
        setup:
        when:
            edgeXReleaseGitTagUtil.validate(validReleaseInfo.findAll {it.key != 'name'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'name\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            edgeXReleaseGitTagUtil.validate(validReleaseInfo.findAll {it.key != 'version'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'version\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a releaseStream attribute" () {
        setup:
        when:
            edgeXReleaseGitTagUtil.validate(validReleaseInfo.findAll {it.key != 'releaseStream'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'releaseStream\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a repo attribute" () {
        setup:
        when:
            edgeXReleaseGitTagUtil.validate(validReleaseInfo.findAll {it.key != 'repo'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'repo\'')
    }

    def "Test getSSHRepoName [Should] return expected #expectedResult [When] called" () {
        setup:
        expect:
            edgeXReleaseGitTagUtil.getSSHRepoName(repo) == expectedResult
        where:
            repo << [
                'https://github.com/edgexfoundry/sample-service.git',
                'git@github.com:edgexfoundry/sample-service.git',
                'https://github.com/edgexfoundry/edgex-go.git',
                'https://github.com/edgexfoundry/device-sdk-go.git',
                'https://github.com/edgexfoundry/app-functions-sdk-go.git'
            ]
            expectedResult << [
                'git@github.com:edgexfoundry/sample-service.git',
                'git@github.com:edgexfoundry/sample-service.git',
                'git@github.com:edgexfoundry/edgex-go.git',
                'git@github.com:edgexfoundry/device-sdk-go.git',
                'git@github.com:edgexfoundry/app-functions-sdk-go.git'
            ]
    }

    def "Test signGitTag [Should] call edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseGitTagUtil.signGitTag('1.2.3', 'sample-service')
        then:
            1 * getPipelineMock('edgeXInfraLFToolsSign.call')([command: 'git-tag', version: 'v1.2.3'])
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
    }

    def "Test signGitTag [Should] echo edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseGitTagUtil.signGitTag('1.2.3', 'sample-service')
        then:
            1 * getPipelineMock('echo').call('edgeXInfraLFToolsSign(command: git-tag version: v1.2.3)')
    }

    def "Test releaseGitTag [Should] catch and and raise error [When] exception occurs" () {
        setup:
            getPipelineMock('edgeXReleaseGitTag.cloneRepo').call(_) >> {
                throw new Exception('Clone Repository Exception')
            }           
        when:
            edgeXReleaseGitTagUtil.releaseGitTag(validReleaseInfo, 'MyCredentials')
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: ERROR occurred releasing git tag: java.lang.Exception: Clone Repository Exception')
    }

    def "Test releaseGitTag [Should] call expected [When] called" () {
        setup:
        when:
            edgeXReleaseGitTagUtil.releaseGitTag(validReleaseInfo, 'MyCredentials')
        then:
            1 * getPipelineMock('edgeXReleaseGitTag.cloneRepo').call('https://github.com/edgexfoundry/sample-service.git', 'master', 'sample-service', 'MyCredentials')
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                    assert _arguments[0][0][0] == 'SEMVER_BRANCH=master'
                }
            1 * getPipelineMock('edgeXReleaseGitTag.setAndSignGitTag').call('sample-service', '1.2.3')
            1 * getPipelineMock('edgeXReleaseGitTag.bumpAndPushGitTag').call('sample-service', '1.2.3', '-pre=dev pre', true)
    }

}