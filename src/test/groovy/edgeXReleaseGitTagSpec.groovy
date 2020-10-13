import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseGitTagSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitTag = null
    def validReleaseInfo
    def validreleaseGitTagOptions

    def setup() {
        edgeXReleaseGitTag = loadPipelineScriptForTest('vars/edgeXReleaseGitTag.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXSemver')
        explicitlyMockPipelineVariable('edgeXInfraLFToolsSign')
        explicitlyMockPipelineVariable('edgeXReleaseGitTagUtil')

        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'releaseStream': 'master',
            'repo': 'https://github.com/edgexfoundry/sample-service.git'
        ]
        validreleaseGitTagOptions = [
            'credentials': 'edgex-jenkins-ssh',
            'bump': true,
            'tag': true
        ]
        def environmentVariables = [
            'WORKSPACE': '/w/thecars'
        ]
        edgeXReleaseGitTag.getBinding().setVariable('env', environmentVariables)
    }

    def "Test cloneRepo [Should] call sh and sshagent with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('edgeXReleaseGitTagUtil.getSSHRepoName').call('http-repo-name') >> 'git-repo-name'
        when:
            edgeXReleaseGitTag.cloneRepo('http-repo-name', 'master', 'sample-service', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git clone -b master git-repo-name /w/thecars/sample-service || true')
    }

    def "Test cloneRepo [Should] echo sh and sshagent with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
            getPipelineMock('edgeXReleaseGitTagUtil.getSSHRepoName').call('http-repo-name') >> 'git-repo-name'
        when:
            edgeXReleaseGitTag.cloneRepo('http-repo-name', 'master', 'sample-service', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('echo').call('sh git clone -b master git-repo-name /w/thecars/sample-service || true')
    }

    def "Test setAndSignGitTag [Should] call edgeXSemver init, tag and edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseGitTag.setAndSignGitTag('sample-service', '1.2.3')
        then:
            1 * getPipelineMock('edgeXSemver.call')('init', '1.2.3')
            1 * getPipelineMock('edgeXSemver.call')('tag -force')
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
            1 * getPipelineMock('edgeXReleaseGitTagUtil.signGitTag').call('1.2.3', 'sample-service')
    }

    def "Test setAndSignGitTag [Should] echo edgeXSemver init, tag and edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseGitTag.setAndSignGitTag('sample-service', '1.2.3')
        then:
            1 * getPipelineMock('echo').call("edgeXSemver init 1.2.3")
            1 * getPipelineMock('echo').call("edgeXSemver tag -force")
            1 * getPipelineMock('edgeXReleaseGitTagUtil.signGitTag').call('1.2.3', 'sample-service')
    }

    def "Test bumpAndPushGitTag [Should] call edgeXSemver bump and push [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseGitTag.bumpAndPushGitTag('sample-service', '1.2.3', 'pre')
        then:
            1 * getPipelineMock('edgeXSemver.call')('push')
            1 * getPipelineMock('edgeXSemver.call')('bump pre')
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
    }

    def "Test bumpAndPushGitTag [Should] echo edgeXSemver bump and push [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseGitTag.bumpAndPushGitTag('sample-service', '1.2.3', 'pre')
        then:
            1 * getPipelineMock('echo').call('edgeXSemver bump pre\nedgeXSemver push')
    }

    def "Test edgeXReleaseGitTag [Should] call expected [When] called" () {
        setup:
        when:
            edgeXReleaseGitTag(validReleaseInfo,validreleaseGitTagOptions)
        then:
            1 * getPipelineMock('edgeXReleaseGitTagUtil.validate').call(validReleaseInfo)
            1 * getPipelineMock('edgeXReleaseGitTagUtil.releaseGitTag').call(validReleaseInfo, 'edgex-jenkins-ssh', true, true)
    }


}