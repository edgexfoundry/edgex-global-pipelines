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
            explicitlyMockPipelineStep('error')
            explicitlyMockPipelineStep('withEnv')
            def environmentVariables = [
                'SILO': 'MySilo',
                'JENKINS_HOSTNAME': 'MyJenkinsHostname',
                'JOB_NAME': 'MyJobName',
                'BUILD_NUMBER': 'MyBuildNumber',
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXNexusPublish.getBinding().setVariable('env', environmentVariables)
            def String[] zipFiles = ['ZipFile1', 'ZipFile2', 'ZipFile3']
            edgeXNexusPublish.getBinding().setVariable('NEXUS_URL', 'MyNexusUrl')
            edgeXNexusPublish.getBinding().setVariable('NEXUS_REPO', 'MyNexusRepo')
            edgeXNexusPublish.getBinding().setVariable('NEXUS_PATH', 'MyNexusPath')
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
            1 * getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-lftools-log-publisher:x86_64') >> explicitlyMockPipelineVariable('DockerImageMock')
            // NOTE: doesn't seem that withEnv mocks environment variables - we still need to explicitly set the variables in setup
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile1')
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile2')
            1 * getPipelineMock('sh').call('lftools deploy nexus-zip MyNexusUrl MyNexusRepo MyNexusPath ZipFile3')
    }

    def "Test edgeXNexusPublish [Should] raise error [When] config does not include a serverId parameter" () {
        setup:
            explicitlyMockPipelineStep('error')
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
            explicitlyMockPipelineStep('error')
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
            explicitlyMockPipelineStep('error')
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
            explicitlyMockPipelineStep('error')
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
