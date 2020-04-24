import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseGitTagSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitTag = null
    def validReleaseInfo

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXReleaseGitTag = loadPipelineScriptForTest('vars/edgeXReleaseGitTag.groovy')
        edgeXReleaseGitTag.getBinding().setVariable('edgex', {})
        explicitlyMockPipelineStep('isDryRun')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('echo')
        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'releaseStream': 'master',
            'repo': 'https://github.com/edgexfoundry/sample-service.git'
        ]
        def environmentVariables = [
            'WORKSPACE': '/w/thecars'
        ]
        edgeXReleaseGitTag.getBinding().setVariable('env', environmentVariables)
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a name attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseGitTag.validate(validReleaseInfo.findAll {it.key != 'name'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'name\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseGitTag.validate(validReleaseInfo.findAll {it.key != 'version'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'version\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a releaseStream attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseGitTag.validate(validReleaseInfo.findAll {it.key != 'releaseStream'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'releaseStream\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a repo attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseGitTag.validate(validReleaseInfo.findAll {it.key != 'repo'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: Release yaml does not contain \'repo\'')
    }

    def "Test getSSHRepoName [Should] return expected #expectedResult [When] called" () {
        setup:
        expect:
            edgeXReleaseGitTag.getSSHRepoName(repo) == expectedResult
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

    def "Test cloneRepo [Should] call sh and sshagent with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('sshagent')
        when:
            edgeXReleaseGitTag.cloneRepo('https://github.com/edgexfoundry/sample-service.git', 'master', 'sample-service', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git clone -b master git@github.com:edgexfoundry/sample-service.git /w/thecars/sample-service')
    }

    def "Test cloneRepo [Should] echo sh and sshagent with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
            explicitlyMockPipelineStep('sshagent')
        when:
            edgeXReleaseGitTag.cloneRepo('https://github.com/edgexfoundry/sample-service.git', 'master', 'sample-service', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('echo.call')('sh git clone -b master git@github.com:edgexfoundry/sample-service.git /w/thecars/sample-service')
    }

    def "Test setAndSignGitTag [Should] call edgeXSemver init, tag and edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
            explicitlyMockPipelineStep('dir')
        when:
            edgeXReleaseGitTag.setAndSignGitTag('sample-service', '1.2.3')
        then:
            1 * getPipelineMock('edgeXSemver').call('init -ver=1.2.3 -force')
            1 * getPipelineMock('edgeXSemver').call('tag -force')
            1 * getPipelineMock('edgeXInfraLFToolsSign').call([command: 'git-tag', version: 'v1.2.3'])
            2 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
    }

    def "Test setAndSignGitTag [Should] echo edgeXSemver init, tag and edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
        when:
            edgeXReleaseGitTag.setAndSignGitTag('sample-service', '1.2.3')
        then:
            1 * getPipelineMock('echo.call')('edgeXSemver init -ver=1.2.3 -force\nedgeXSemver tag -force')
            1 * getPipelineMock('echo.call')('edgeXInfraLFToolsSign(command: git-tag version: v1.2.3)')
    }

    def "Test signGitTag [Should] call edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
            explicitlyMockPipelineStep('dir')
        when:
            edgeXReleaseGitTag.signGitTag('1.2.3', 'sample-service')
        then:
            1 * getPipelineMock('edgeXInfraLFToolsSign').call([command: 'git-tag', version: 'v1.2.3'])
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
    }

    def "Test signGitTag [Should] echo edgeXInfraLFToolsSign with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
        when:
            edgeXReleaseGitTag.signGitTag('1.2.3', 'sample-service')
        then:
            1 * getPipelineMock('echo.call')('edgeXInfraLFToolsSign(command: git-tag version: v1.2.3)')
    }

    def "Test bumpAndPushGitTag [Should] call edgeXSemver bump and push [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('dir')
        when:
            edgeXReleaseGitTag.bumpAndPushGitTag('sample-service', '1.2.3', 'pre')
        then:
            1 * getPipelineMock('edgeXSemver').call('push')
            1 * getPipelineMock('edgeXSemver').call('bump pre')
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'sample-service' == _arguments[0][0]
            }
    }

    def "Test bumpAndPushGitTag [Should] echo edgeXSemver bump and push [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
        when:
            edgeXReleaseGitTag.bumpAndPushGitTag('sample-service', '1.2.3', 'pre')
        then:
            1 * getPipelineMock('echo.call')('edgeXSemver bump pre\nedgeXSemver push')
    }

    def "Test releaseGitTag [Should] catch and and raise error [When] exception occurs" () {
        setup:
            explicitlyMockPipelineStep('error')
            // TODO: figure out how to properly stub cloneRepo to set side-effect Exception
            // explicitlyMockPipelineVariable('cloneRepo')
            // getPipelineMock('cloneRepo').call(_) >> {
            //     throw new Exception('Clone Repository Exception')
            // }           
            explicitlyMockPipelineStep('sshagent')
            getPipelineMock('sshagent')(_) >> {
                throw new Exception('SSH Exception')
            }
        when:
            edgeXReleaseGitTag.releaseGitTag(validReleaseInfo, 'MyCredentials')
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseGitTag]: ERROR occurred releasing git tag: java.lang.Exception: SSH Exception')
    }

    def "Test edgeXReleaseGitTag [Should] not throw error [When] called with valid release info and DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('sshagent')
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
            explicitlyMockPipelineStep('dir')
        when:
            edgeXReleaseGitTag(validReleaseInfo)
        then:
            noExceptionThrown()
    }

    def "Test edgeXReleaseGitTag [Should] call edgeXSemver bump with default [When] called" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('sshagent')
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
            explicitlyMockPipelineStep('dir')
        when:
            edgeXReleaseGitTag(validReleaseInfo)
        then:
            1 * getPipelineMock('edgeXSemver').call('bump -pre=dev pre')
    }

    def "Test edgeXReleaseGitTag [Should] call edgeXSemver bump [When] called with semverBumpLevel" () {
        setup:
            getPipelineMock('isDryRun')() >> false
            explicitlyMockPipelineStep('sshagent')
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
            explicitlyMockPipelineStep('dir')
        when:
            validReleaseInfo.semverBumpLevel = '-pre=exp pre'
            edgeXReleaseGitTag(validReleaseInfo)
        then:
            1 * getPipelineMock('edgeXSemver').call('bump -pre=exp pre')
    }

}