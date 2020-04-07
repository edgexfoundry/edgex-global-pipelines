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
        explicitlyMockPipelineVariable('out')
        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'releaseStream': 'master',
            'repo': 'https://github.com/edgexfoundry/sample-service.git'
        ]
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

    def "Test cloneRepo [Should] call sh and sshagent with expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('sshagent')
        when:
            edgeXReleaseGitTag.cloneRepo('https://github.com/edgexfoundry/sample-service.git', 'master', 'sample-service', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git clone -b master git@github.com:edgexfoundry/sample-service.git sample-service')
            1 * getPipelineMock('sh').call('cd sample-service')
    }

    def "Test setGitTag [Should] call edgeXSemver init, tag and push with expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('edgeXSemver')
        when:
            edgeXReleaseGitTag.setGitTag('sample-service', '1.2.3')
        then:
            1 * getPipelineMock('edgeXSemver').call('init -ver=1.2.3 -force')
            1 * getPipelineMock('edgeXSemver').call('tag -force')
            1 * getPipelineMock('edgeXSemver').call('push')
    }

    def "Test signGitTag [Should] call edgeXInfraLFToolsSign with expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
        when:
            edgeXReleaseGitTag.signGitTag('1.2.3')
        then:
            1 * getPipelineMock('edgeXInfraLFToolsSign').call([command: 'git-tag', version: 'v1.2.3'])
    }

    def "Test releaseGitTag [Should] catch and echo exception message [When] exception occurs" () {
        setup:
            explicitlyMockPipelineVariable('echo')
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
            1 * getPipelineMock('echo.call')('[edgeXReleaseGitTag]: ERROR occurred releasing git tag: java.lang.Exception: SSH Exception')
    }

    def "Test edgeXReleaseGitTag [Should] not throw error [When] called with valid release info" () {
        setup:
            explicitlyMockPipelineVariable('echo')
            explicitlyMockPipelineStep('sshagent')
            explicitlyMockPipelineStep('edgeXSemver')
            explicitlyMockPipelineStep('edgeXInfraLFToolsSign')
        when:
            edgeXReleaseGitTag(validReleaseInfo)
        then:
            noExceptionThrown()
    }

}