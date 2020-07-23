import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSemverSpec extends JenkinsPipelineSpecification {

    def edgeXSemver = null

    def setup() {
        edgeXSemver = loadPipelineScriptForTest('vars/edgeXSemver.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXSemver [Should] call expected and set VERSION envvar [When] called with no command" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver()
        then:
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            1 * getPipelineMock('sh')([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
            environmentVariables['VERSION'] == '1.2.3-dev.4'
    }

    def "Test edgeXSemver [Should] call expected and set VERSION envvar [When] called with init command, semverVersion and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver('init', '1.2.4')
        then:
            // verify docker.image
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            // verify docker.image.inside arguments
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '-v /etc/ssh:/etc/ssh'
                assert dockerArgs == _arguments[0][0]
            }
            // verify withEnv envvars
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SSH_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts',
                    'SEMVER_DEBUG=on'
                ]
                assert envArgs == _arguments[0][0]
            }
            // verify git semver command
            1 * getPipelineMock('sh').call('git semver init -ver=1.2.4 -force')
            environmentVariables['VERSION'] == '1.2.4'
            // verify writefile
            1 * getPipelineMock('writeFile').call([file: 'VERSION', text: '1.2.4'])
    }

    def "Test edgeXSemver [Should] get semver version from command and set VERSION envvar [When] called with init command" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver('init')
        then:
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            1 * getPipelineMock('sh').call([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
            environmentVariables['VERSION'] == '1.2.3-dev.4'
    }

    @Ignore
    def "Test edgeXSemver [Should] call expected [When] called with tag force command and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver('tag -force')
        then:
            // TODO: this is a bug - tag force is not being executed because GITSEMVER_HEAD_TAG is set
            // https://github.com/edgexfoundry/cd-management/issues/55
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            1 * getPipelineMock('sh').call('git semver tag -force')
            1 * getPipelineMock('sh').call([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
    }

    def "Test executeSSH [Should] call sshagent and sh with the expected arguments [When] called" () {
        setup:
        when:
            edgeXSemver.executeSSH('MyCredentials', 'MyCommand')
        then:   
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('MyCommand')
    }

    def "Test setHeadTagEnv [Should] not call sshagent [When] GITSEMVER_HEAD_TAG environment variable is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyGitsemverHeadTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver.setHeadTagEnv('MyCredentials')
        then:
            0 * getPipelineMock('sshagent').call(_) 
    }

    def "Test setHeadTagEnv [Should] query for and set GITSEMVER_HEAD_TAG environment variable [When] GITSEMVER_HEAD_TAG environment variable is not set" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver.setHeadTagEnv('MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh')([script:'git describe --exact-match --tags HEAD', returnStdout:true]) >> 'MyTag\n'
            environmentVariables['GITSEMVER_HEAD_TAG'] == 'MyTag'
    }

    def "Test setHeadTagEnv [Should] not set GITSEMVER_HEAD_TAG environment variable [When] exception occurs within ssh" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')(_) >> {
                throw new Exception('MockedException1')
            }
        when:
            edgeXSemver.setHeadTagEnv('MyCredentials')
        then:
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
    }
}
