import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerImageSpec extends JenkinsPipelineSpecification {
    def edgeXReleaseDockerImage = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) {
            super( _message );
        }
    }

    def setup() {
        edgeXReleaseDockerImage = loadPipelineScriptForTest('vars/edgeXReleaseDockerImage.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('error')
    }

    @Ignore
    def "Test edgeXReleaseDockerImage [Should] raise error [When] config is empty" () {
        setup:

        when:
            try {
                def releaseYaml = [:]
                edgeXReleaseDockerImage(releaseYaml)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerSource'")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerDestination'")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'releaseStream' (branch where you are releasing from). Example: master")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain release 'version'. Example: v1.1.2")
    }

    @Ignore
    def "Test edgeXReleaseDockerImage [Should] run sh commands to tag and push with the expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('echo') //temporary until final impl.
            explicitlyMockPipelineVariable("edgeXDocker")
        when:
            def releaseYaml = [
                name:'app-functions-sdk-go',
                version:'v1.2.0', 
                releaseName:'geneva', 
                releaseStream:'master',
                repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                gitTag:false,
                gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                dockerImages:true,
                dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go'], 
                dockerDestination:[
                    'nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go',
                    'edgexfoundry/app-functions-sdk-go'
                ],
                snap:false,
                snapDestination:'https://snapcraft.org/..',
                snapChannel:'release/stable'
            ]

            try {
                edgeXReleaseDockerImage.publishDockerImages(releaseYaml)
            } catch(TestException exception) { }
        then:
            println "Coming soon"

    }


    def "Test edgeXReleaseDockerImage.isValidReleaseRegistry [Should] return valid docker release target [When] called " () {
        setup:
        expect:
            [
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'docker.io', namespace: 'edgexfoundry', image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'docker.io', namespace: null, image: 'python', tag: '3-alpine']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10002', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10004', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10003', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'example.com', namespace: null, image: 'sample-service'])
            ] == expectedResult
        where:
            expectedResult = [
                true,
                false,
                true,
                false,
                false,
                false
            ]
    }
}