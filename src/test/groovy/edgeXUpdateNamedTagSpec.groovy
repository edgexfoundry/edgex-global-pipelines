import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXUpdateNamedTagSpec extends JenkinsPipelineSpecification {
    
    def edgeXUpdateNamedTag = null

    def setup() {
        edgeXUpdateNamedTag = loadPipelineScriptForTest('vars/edgeXUpdateNamedTag.groovy')

        explicitlyMockPipelineVariable('edgex')
    }

    def "Test edgeXUpdateNamedTag [Should] raise error [When] the original version [ogVersion] is not supplied" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXUpdateNamedTag()
        then:
            1 * getPipelineMock('error').call('[edgeXUpdateNamedTag]: Original version (ogVersion) is required for the update named tag script.')
    }

    def "Test edgeXUpdateNamedTag [Should] raise error [When] the named version [namedVersion] is not supplied" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXUpdateNamedTag('0.0.0')
        then:
            1 * getPipelineMock('error').call('[edgeXUpdateNamedTag]: Named version (namedVersion) is required for the update named tag script.')
    }

    /* Commenting out until isDryRun is avilable outside of edgeXRelease* functions
    def "Test edgeXUpdateNamedTag [Should] call sh and sshagent with expected arguments [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('isDryRun')() >> false
        when:
            edgeXUpdateNamedTag('0.0.0', 'experimental')
        then:
            1 * getPipelineMock('sh').call('echo y | ./update-named-tag.sh v0.0.0 experimental')
    }

    def "Test edgeXUpdateNamedTag [Should] echo and sshagent with expected arguments [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
        when:
            edgeXUpdateNamedTag('0.0.0', 'experimental')
        then:
            1 * getPipelineMock('echo.call')('echo y | ./update-named-tag.sh v0.0.0 experimental')
    }*/

}