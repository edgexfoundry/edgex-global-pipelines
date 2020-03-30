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
                def config = [:]
                edgeXReleaseDockerImage(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Please provide source image. Example: from: 'nexus3.edgexfoundry.org:10004/sample:master'")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Please provide release target: Available targets: release, dockerhub }")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Please provide release version. Example: v1.1.2")
    }

    @Ignore
    def "Test edgeXReleaseDockerImage [Should] raise error [When] config does not include a to and version parameters" () {
        setup:

        when:
            try {
                def config = [
                    from: 'nexus3.edgexfoundry.org:10004/sample-service:master'
                ]
                edgeXReleaseDockerImage(config)
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Please provide release target: Available targets: release, dockerhub }")
            1 * getPipelineMock('error').call("[edgeXReleaseDockerImage] Please provide release version. Example: v1.1.2")
    }

    def "Test edgeXReleaseDockerImage [Should] call and sh with the expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('echo') //temporary until final impl.
        when:
            def config = [
                from: 'nexus3.edgexfoundry.org:10004/sample-service:master',
                to: 'edgexfoundry/sample-service',
                version: 'v0.0.1-test'
            ]

            try {
                edgeXReleaseDockerImage.call(config)
            } catch(TestException exception) { }
        then:
            1 * getPipelineMock('echo').call('docker tag nexus3.edgexfoundry.org:10004/sample-service:master docker.io/edgexfoundry/sample-service:v0.0.1-test')
            1 * getPipelineMock('echo').call('docker push docker.io/edgexfoundry/sample-service:v0.0.1-test')
    }


    def "Test edgeXReleaseDockerImage.getReleaseTarget [Should] return valid docker release target [When] called " () {
        setup:
        expect:
            [
                edgeXReleaseDockerImage.getReleaseTarget('edgexfoundry/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('docker.io/edgexfoundry/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('nexus3.edgexfoundry.org:10002/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('nexus3.edgexfoundry.org:10004/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('nexus3.edgexfoundry.org:10003/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('example.com/sample-service'),
                edgeXReleaseDockerImage.getReleaseTarget('docker.io/python/sample-service')
            ] == expectedResult
        where:
            expectedResult = [
                'docker.io/edgexfoundry',
                'docker.io/edgexfoundry',
                'nexus3.edgexfoundry.org:10002',
                null,
                null,
                null,
                null
            ]
    }

   def "Test edgeXReleaseDockerImage [Should] return versioned imaged [When] called " () {
       setup:
       expect:
           [
           edgeXReleaseDockerImage.getFinalImageWithTag(
               'nexus3.edgexfoundry.org:10004/sample-service:master',
               'docker.io/edgexfoundry',
               'v1.1.2'
           ),
           edgeXReleaseDockerImage.getFinalImageWithTag(
               'nexus3.edgexfoundry.org:10004/sample-service:master',
               'nexus3.edgexfoundry.org:10002',
               'v1.1.3'
           )] == expectedResult
       where:
           expectedResult = [
               'docker.io/edgexfoundry/sample-service:v1.1.2',
               'nexus3.edgexfoundry.org:10002/sample-service:v1.1.3'
           ]
   }
}