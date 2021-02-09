import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXEmailSpec extends JenkinsPipelineSpecification {
    def edgeXEmail = null

    def setup() {
        edgeXEmail = loadPipelineScriptForTest('vars/edgeXEmail.groovy')
        explicitlyMockPipelineVariable('edgeXEmailUtil')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXEmail [Should] send email [When] called with emailTo" () {
        setup:
            edgeXEmail.getBinding().setVariable('currentBuild', [ result: 'FAILURE' ])

            getPipelineMock('edgeXEmailUtil.generateEmailTemplate').call() >> {
                return 'Mock Email Template'
            }
        when:
            edgeXEmail(subject: '[FAILURE] Mock Email', emailTo: 'mock@edgexfoundry.org')
        then:
            1 * getPipelineMock('mail').call(body: 'Mock Email Template', subject: '[FAILURE] Mock Email', to: 'mock@edgexfoundry.org', mimeType: 'text/html')
    }

    def "Test edgeXEmail [Should] NOT send email [When] called without emailTo" () {
        setup:
            edgeXEmail.getBinding().setVariable('currentBuild', [ result: 'FAILURE' ])

            getPipelineMock('edgeXEmailUtil.generateEmailTemplate').call() >> {
                return 'Mock Email Template'
            }
        when:
            edgeXEmail(subject: '[FAILURE] Mock Email', emailTo: null)
        then:
            0 * getPipelineMock('mail').call(body: 'Mock Email Template', subject: '[FAILURE] Mock Email', to: null, mimeType: 'text/html')
    }
}
