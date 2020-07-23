import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXDockerLoginSpec extends JenkinsPipelineSpecification {

    def edgeXDockerLogin = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {

        edgeXDockerLogin = loadPipelineScriptForTest('vars/edgeXDockerLogin.groovy')
    }

    def "Test call [Should] raise error [When] config has no settingsFile" () {
        setup:
        when:
            try {
                edgeXDockerLogin()
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test call [Should] raise error [When] config has dockerRegistry but no dockerRegistryPorts" () {
        setup:
        when:
            try {
                def config = [
                    'settingsFile': 'settingsFile',
                    'dockerRegistry': 'dockerRegistry'
                ]
                edgeXDockerLogin(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test call [Should] raise error [When] config has dockerRegistryPorts but no dockerRegistry" () {
        setup:
        when:
            try {
                def config = [
                    'settingsFile': 'settingsFile',
                    'dockerRegistryPorts': 'dockerRegistryPorts'
                ]
                edgeXDockerLogin(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test call [Should] call expected [When] called" () {
        setup:
        when:
            try {
                def config = [
                    'settingsFile': 'settingsFile',
                    'dockerRegistry': 'dockerRegistry',
                    'dockerRegistryPorts': 'dockerRegistryPorts',
                    'dockerHubRegistry': 'dockerHubRegistry',
                    'dockerHubEmail': 'dockerHubEmail'
                ]
                edgeXDockerLogin(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'DOCKER_REGISTRY=dockerRegistry',
                    'REGISTRY_PORTS=dockerRegistryPorts',
                    'DOCKERHUB_REGISTRY=dockerHubRegistry',
                    'DOCKERHUB_EMAIL=dockerHubEmail'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock('libraryResource').call('global-jjb-shell/docker-login.sh')
            1 * getPipelineMock("configFile.call").call(['fileId':'settingsFile', 'variable':'SETTINGS_FILE'])
    }

}
