import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerImageSpec extends JenkinsPipelineSpecification {
    def edgeXReleaseDockerImage
    def validReleaseYaml, invalidReleaseYaml
    
    public static class TestException extends RuntimeException {
        public TestException(String _message) {
            super( _message );
        }
    }

    def setup() {
        edgeXReleaseDockerImage = loadPipelineScriptForTest('vars/edgeXReleaseDockerImage.groovy')

        def edgeXDocker = loadPipelineScriptForTest('vars/edgeXDocker.groovy')
        edgeXReleaseDockerImage.getBinding().setVariable('edgeXDocker', edgeXDocker)

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('error')
        explicitlyMockPipelineStep('echo')

        validReleaseYaml = [
            name:'app-functions-sdk-go',
            version:'v1.2.0',
            releaseName:'geneva',
            releaseStream:'master',
            repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
            dockerImages:true,
            dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go'],
            dockerDestination:[
                'nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go',
                'edgexfoundry/app-functions-sdk-go'
            ]
        ]

        invalidReleaseYaml = [
            name:'app-functions-sdk-go',
            version:'v1.2.0',
            releaseName:'geneva',
            releaseStream:'master',
            repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
            dockerImages:true,
            dockerSource:['nexus3.edgexfoundry.org:10003/docker-app-functions-sdk-go'],
            dockerDestination:[
                'nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go', // invalid destination
                'https://example.com/edgexfoundry/app-functions-sdk-go'      // invalid destination
            ]
        ]
    }

    @Ignore
    def "Test edgeXReleaseDockerImage [Should] raise error [When] config is empty" () {
        setup:

        when:
            try {
                def validReleaseYaml = [:]
                edgeXReleaseDockerImage(validReleaseYaml)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerSource'")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'dockerDestination'")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain 'releaseStream' (branch where you are releasing from). Example: master")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Release yaml does not contain release 'version'. Example: v1.1.2")
    }

    def "Test edgeXReleaseDockerImage [Should] run echo commands to tag and push when DRY_RUN is true [When] called" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'true'
            ]
            edgeXReleaseDockerImage.getBinding().setVariable('env', environmentVariables)
        when:

            try {
                edgeXReleaseDockerImage.publishDockerImages(validReleaseYaml)
            } catch(TestException exception) { }
        then:
            1 * getPipelineMock('echo').call("docker tag nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:master nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go:v1.2.0")
            1 * getPipelineMock('echo').call("docker push nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go:v1.2.0")

            1 * getPipelineMock('echo').call("docker tag nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:master docker.io/edgexfoundry/app-functions-sdk-go:v1.2.0")
            1 * getPipelineMock('echo').call("docker push docker.io/edgexfoundry/app-functions-sdk-go:v1.2.0")
    }

    def "Test edgeXReleaseDockerImage [Should] run sh commands to tag and push when DRY_RUN is false [When] called" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'false'
            ]
            edgeXReleaseDockerImage.getBinding().setVariable('env', environmentVariables)

        when:
            try {
                edgeXReleaseDockerImage.publishDockerImages(validReleaseYaml)
            } catch(TestException exception) { }
        then:
            1 * getPipelineMock('sh').call("docker tag nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:master nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go:v1.2.0")
            1 * getPipelineMock('sh').call("docker push nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go:v1.2.0")

            1 * getPipelineMock('sh').call("docker tag nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:master docker.io/edgexfoundry/app-functions-sdk-go:v1.2.0")
            1 * getPipelineMock('sh').call("docker push docker.io/edgexfoundry/app-functions-sdk-go:v1.2.0")

            1 * getPipelineMock('echo').call("[edgeXReleaseDockerImage] Successfully published [2] images")
    }

    def "Test edgeXReleaseDockerImage [Should] error [When] invalid yaml configuration is used" () {
        setup:

        when:
            try {
                edgeXReleaseDockerImage.publishDockerImages(invalidReleaseYaml)
            } catch(TestException exception) { }
        then:
            1 * getPipelineMock('echo').call("[edgeXReleaseDockerImage] The sourceImage [nexus3.edgexfoundry.org:10003/docker-app-functions-sdk-go:master] did not release...No corresponding dockerDestination entry found.")
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