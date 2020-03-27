import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerNexusSpec extends JenkinsPipelineSpecification {
    
   def edgeXReleaseDockerNexus = null

   def setup() {
        edgeXReleaseDockerNexus = loadPipelineScriptForTest('vars/edgeXReleaseDockerNexus.groovy')
        explicitlyMockPipelineVariable('out')
    }
   def "Test edgeXDockerTag [Should] raise error [When] config does not include a dockerImageRepo parameter" () {
       setup:
           explicitlyMockPipelineStep('error')
       when:
           try {
               def config = [
                   dockerImageRepo: ''
               ]
               edgeXDockerTag(config)
           }
           catch(TestException exception) {
           }
       then:
           1 * getPipelineMock('error').call()
   }
   def "Test call [Should] return expected map of overriden values [When] custom config" () {
       setup:
           getPipelineMock('edgex.defaultTrue')(null) >> {
               false
           }
       expect:
           edgeXDockerTag.call() == expectedResult
       where:
           config << [
               [
                   snapshots: 10003,
                    snapshot: 10003,
                    staging: 10004,
                    release: 10002
               ]
           ]
           expectedResult << [
               [
                   snapshots: 10003,
                    snapshot: 10003,
                    staging: 10004,
                    release: 10002
               ]
           ]
   }
   def "Test edgeXDockerTag [Should] return [When] called " () {
       setup:
           explicitlyMockPipelineStep('error')
       when:
           try {
               def config = [
               [
                   branchName: 'master',
                   version: 'v0.0.1-test',
                   dockerNexusURL: 'nexus3edgexfoundry.org',
                   dockerImageRepo: 'sample-service'
                   from: 'staging'
                   to: 'release'
               ],
               [
                   snapshots: 10003,
                    snapshot: 10003,
                    staging: 10004,
                    release: 10002
               ]
               ]
               edgeXDockerTag(config)
           }
           catch(TestException exception) {
           }
       then:
           1 * getPipelineMock('sh').call('docker pull ${dockerNexusURL}:${from}/${dockerImageRepo}:${branchName}')
           1 * getPipelineMock('sh').call('docker tag ${imageID} ${dockerNexusURL}:${to}/${dockerImageRepo}:${version}')
           1 * getPipelineMock('sh').call('docker push ${dockerNexusURL}:${to}/${dockerImageRepo}:${version}')
   }
}
