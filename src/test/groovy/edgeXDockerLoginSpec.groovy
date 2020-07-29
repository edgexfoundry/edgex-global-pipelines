import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXDockerLoginSpec extends JenkinsPipelineSpecification {

    def edgeXDockerLogin = null

    def setup() {

        edgeXDockerLogin = loadPipelineScriptForTest('vars/edgeXDockerLogin.groovy')
    }

    def "Test call [Should] raise error [When] config has no settingsFile" () {
        setup:
        when:
            edgeXDockerLogin()
        then:
            1 * getPipelineMock('error').call('Project Settings File id (settingsFile) is required for the docker login script.')
    }

    def "Test call [Should] raise error [When] config has dockerRegistry but no dockerRegistryPorts" () {
        setup:
        when:
            def config = [
                'settingsFile': 'settingsFile',
                'dockerRegistry': 'dockerRegistry'
            ]
            edgeXDockerLogin(config)
        then:
            1 * getPipelineMock('error').call('Docker registry ports (dockerRegistryPorts) are required when docker registry is set (dockerRegistry).')
    }

    def "Test call [Should] raise error [When] config has dockerRegistryPorts but no dockerRegistry" () {
        setup:
        when:
            def config = [
                'settingsFile': 'settingsFile',
                'dockerRegistryPorts': 'dockerRegistryPorts'
            ]
            edgeXDockerLogin(config)
        then:
            1 * getPipelineMock('error').call('Docker registry (dockerRegistry) is required when docker registry ports are set (dockerRegistryPorts).')
    }

    def "Test call [Should] call expected [When] called" () {
        setup:
        when:
            def config = [
                'settingsFile': 'settingsFile',
                'dockerRegistry': 'dockerRegistry',
                'dockerRegistryPorts': 'dockerRegistryPorts',
                'dockerHubRegistry': 'dockerHubRegistry',
                'dockerHubEmail': 'dockerHubEmail'
            ]
            edgeXDockerLogin(config)
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
