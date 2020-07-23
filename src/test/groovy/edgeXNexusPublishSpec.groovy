import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXNexusPublishSpec extends JenkinsPipelineSpecification {

    def edgeXNexusPublish = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXNexusPublish = loadPipelineScriptForTest('vars/edgeXNexusPublish.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXNexusPublish [Should] call lftools deploy for every zipFile [When] called" () {
        setup:          
            def environmentVariables = [
                'SILO': 'MySilo',
                'JENKINS_HOSTNAME': 'MyJenkinsHostname',
                'JOB_NAME': 'MyJobName',
                'BUILD_NUMBER': 'MyBuildNumber',
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'NEXUS_URL': 'MyNexusUrl',
                'NEXUS_REPO': 'MyNexusRepo',
                'NEXUS_PATH': 'MyNexusPath'
            ]
            edgeXNexusPublish.getBinding().setVariable('env', environmentVariables)
            def String[] zipFiles = ['ZipFile1', 'ZipFile2', 'ZipFile3']

        when:
            def config = [
                serverId: 'MyServerId',
                mavenSettings: 'MyMavenSettings',
                nexusRepo: 'MyNexusRepo',
                zipFilePath: 'MyZipFilePath'
            ]
            edgeXNexusPublish(config)

        then:
            1 * getPipelineMock('sh')([script: 'uname -m', returnStdout: true]) >> '\n'
            1 * getPipelineMock('findFiles').call([glob: 'MyZipFilePath']) >> zipFiles
            // verify docker.image
            1 * getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-lftools-log-publisher:x86_64') >> explicitlyMockPipelineVariable('DockerImageMock')
            // verify docker.image.inside arguments
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '-u 0:0'
                assert dockerArgs == _arguments[0][0]
            }
            // verify withEnv envvars
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SERVER_ID=MyServerId',
                    'NEXUS_REPO=MyNexusRepo',
                    'NEXUS_PATH=MySilo/MyJenkinsHostname/MyJobName/MyBuildNumber'
                ]
                assert envArgs == _arguments[0][0]
            }
            // NOTE: careful here NEXUS_PATH is determined at run-time within the withEnv clause but its value is different
            // below because the env var was mocked with a default value in setup
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile1')
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile2')
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile3')
    }

    def "Test edgeXNexusPublish [Should] raise error [When] config does not include a serverId parameter" () {
        setup:
            getPipelineMock('sh')([script: 'uname -m', returnStdout: true]) >> '\n'
            def String[] zipFiles = []
        when:
            try {
                def config = [
                    path: 'MyNexusPath'
                ]
                edgeXNexusPublish(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('findFiles').call([glob: null]) >> zipFiles
            1 * getPipelineMock('error').call("[edgeXNexusPublish] serverId is required to publish to nexus. Example: 'logs'")
    }

    def "Test edgeXNexusPublish [Should] raise error [When] config does not include a mavenSettings parameter" () {
        setup:
            getPipelineMock('sh')([script: 'uname -m', returnStdout: true]) >> '\n'
            def String[] zipFiles = []
        when:
            try {
                def config = [
                    path: 'MyNexusPath',
                    serverId: 'MyServerId'
                ]
                edgeXNexusPublish(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('findFiles').call([glob: null]) >> zipFiles
            1 * getPipelineMock('error').call("[edgeXNexusPublish] mavenSettings is required to publish to nexus. Example: 'sandbox-settings'")
    }

    def "Test edgeXNexusPublish [Should] raise error [When] config does not include a nexusRepo parameter" () {
        setup:
            getPipelineMock('sh')([script: 'uname -m', returnStdout: true]) >> '\n'
            def String[] zipFiles = []
        when:
            try {
                def config = [
                    path: 'MyNexusPath',
                    serverId: 'MyServerId',
                    mavenSettings: 'MySetting1,MySetting2'
                ]
                edgeXNexusPublish(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('findFiles').call([glob: null]) >> zipFiles
            1 * getPipelineMock('error').call("[edgeXNexusPublish] nexusRepo is required to publish to nexus. Example: 'logs'")
    }

    def "Test edgeXNexusPublish [Should] raise error [When] config does not include a zipFilePath parameter" () {
        setup:
            getPipelineMock('sh')([script: 'uname -m', returnStdout: true]) >> '\n'
            def String[] zipFiles = []
        when:
            try {
                def config = [
                    path: 'MyNexusPath',
                    serverId: 'MyServerId',
                    mavenSettings: 'MySetting1,MySetting2',
                    nexusRepo: 'MyNexusRepo'
                ]
                edgeXNexusPublish(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('findFiles').call([glob: null]) >> zipFiles
            1 * getPipelineMock('error').call("[edgeXNexusPublish] zipFilePath is required to publish to nexus. Example: '**/*.zip'")
    }

}
