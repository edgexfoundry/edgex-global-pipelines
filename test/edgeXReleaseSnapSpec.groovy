import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseSnapSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseSnap = null

    def setup() {
        edgeXReleaseSnap = loadPipelineScriptForTest('vars/edgeXReleaseSnap.groovy')
        explicitlyMockPipelineVariable('out')
    }

    @Ignore
    def "Test edgeXReleaseSnap [Should] return [When] called " () {
        
    }

}