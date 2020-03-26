import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSemverSpec extends JenkinsPipelineSpecification {

    def edgeXSemver = null

    def setup() {

        edgeXSemver = loadPipelineScriptForTest('vars/edgeXSemver.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('sshagent')
    }

    @Ignore
    def "Test edgeXSemver [Should] do expected [When] called with command" () {
        setup:
            explicitlyMockPipelineVariable('env')
            // TODO: figure out how to mock abd verify env.setProperty calls
            // Caused by: groovy.lang.MissingPropertyException: No such property: VERSION for class: com.homeaway.devtools.jenkins.testing.PipelineVariableImpersonato

        when:
            edgeXSemver()
        then:
            1 * getPipelineMock('env.getProperty').call('GITSEMVER_HEAD_TAG') >> 'GitsemverHeadTag'
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            1 * getPipelineMock('sh')([script: 'git semver', returnStdout: true]) >> 'GitSemverVersion\n'
            // 1 * getPipelineMock('env.setProperty')('VERSION', '--version--')
            // noExceptionThrown()
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
            explicitlyMockPipelineVariable('env')
            // def envMock = Mock(Object)
            // edgeXSemver.getBinding().setVariable('env', envMock)
        when:
            edgeXSemver.setHeadTagEnv('MyCredentials')
        then:
            1 * getPipelineMock('env.getProperty').call('GITSEMVER_HEAD_TAG') >> null
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call(['script':'git describe --exact-match --tags HEAD', 'returnStdout':true]) >> 'MyTag\n'

            // TODO: figure out how to verify env.setProperty gets called corectly
            // groovy.lang.MissingPropertyException: No such property: GITSEMVER_HEAD_TAG for class: com.homeaway.devtools.jenkins.testing.PipelineVariableImpersonator
            // groovy.lang.MissingPropertyException: No such property: GITSEMVER_HEAD_TAG for class: org.spockframework.mock.ISpockMockObject$$EnhancerByCGLIB$$1f624ad2
            // 1 * getPipelineMock('env.setProperty').call('GITSEMVER_HEAD_TAG', 'MyTag')
    }

    def "Test setHeadTagEnv [Should] not set GITSEMVER_HEAD_TAG environment variable [When] exception occurs within ssh" () {
        setup:
            getPipelineMock('sh')(_) >> {
                throw new Exception('MockedException1')
            }
        when:
            edgeXSemver.setHeadTagEnv('MyCredentials')
        then:
             0 * getPipelineMock('env.setProperty').call(_)
    }

    def "Test isHeadTagEnv [Should] return true [When] GITSEMVER_HEAD_TAG environment variable is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyGitsemverHeadTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeXSemver.isHeadTagEnv() == true
    }

    def "Test isHeadTagEnv [Should] return false [When] GITSEMVER_HEAD_TAG environment variable is not set and exception occurs within setHeadTagEnv" () {
        setup:
            getPipelineMock('sshagent')(_) >> {
                throw new Exception('MockedException2')
            }
        expect:
            edgeXSemver.isHeadTagEnv() == false
    }

}
