import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore
public class edgeXReleaseDockerNexus extends JenkinsPipelineSpecification {
   def edgeXReleaseDockerNexus = null
   def setup() {
       edgeXReleaseDockerNexus = loadPipelineScriptForTest('vars/edgeXReleaseDockerNexus.groovy')
       explicitlyMockPipelineVariable('out')
   }
   def "Test edgeXReleaseDockerNexus [Should] raise error [When] config does not include a nexusPortMapping parameter and is not an integer value" () {
       setup:
           explicitlyMockPipelineStep('error')
       when:
           try {
               def config = [
                   nexusPortMapping: '10004'.isInteger()
               ]
               edgeXReleaseDockerNexus(config)
           }
           catch(TestException exception) {
           }
       then:
           1 * getPipelineMock('error').call()
   }
   def "Test edgeXReleaseDockerNexus [Should] raise error [When] config does not include a _dockerImageRepo parameter" () {
       setup:
           explicitlyMockPipelineStep('error')
       when:
           try {
               def config = [
                   dockerImageRepo: 'sample-service'
               ]
               edgeXReleaseDockerNexus(config)
           }
           catch(TestException exception) {
           }
       then:
           1 * getPipelineMock('error').call()
   }
   def "Test edgeXReleaseDockerNexus [Should] return [When] called " () {
       setup:
           explicitlyMockPipelineStep('error')
       when:
           try {
               def config = [
                   version: 'v0.0.1-test',
                   dockerNexusURL: 'nexus3edgexfoundry.org',
                   dockerImageRepo: 'sample-service'
                   sourceNexusPort: '10004'
                   destinationNexusPort: '10002'
               ]
                               ]
               edgeXReleaseDockerNexus(config)
           }
           catch(TestException exception) {
           }
       then:
           1 * getPipelineMock().call()
           1 * getPipelineMock('sh').call('docker pull ${dockerNexusURL}:${sourceNexusPort}/${dockerImageRepo}')
           1 * getPipelineMock('sh').call('docker tag ${dockerNexusURL}:${sourceNexusPort}/${dockerImageRepo} ${dockerNexusURL}:${destinationNexusPort}/${_dockerImageRepo}:${_version}')
           1 * getPipelineMock('sh').call('docker push ${dockerNexusURL}:${destinationNexusPort}/${dockerImageRepo}:${version}')
   }
}