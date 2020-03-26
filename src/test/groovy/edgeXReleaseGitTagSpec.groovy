import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseGitTagSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseGitTag = null

    def setup() {
        edgeXReleaseGitTag = loadPipelineScriptForTest('vars/edgeXReleaseGitTag.groovy')
        explicitlyMockPipelineVariable('out')
    }

    @Ignore
    def "Test edgeXReleaseGitTag [Should] return [When] called " () {
        
    }

}