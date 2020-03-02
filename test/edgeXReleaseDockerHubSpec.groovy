import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerHubSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseDockerHub = null

    def setup() {
        edgeXReleaseDockerHub = loadPipelineScriptForTest('vars/edgeXReleaseDockerHub.groovy')
        explicitlyMockPipelineVariable('out')
    }

    @Ignore
    def "Test edgeXReleaseDockerHub [Should] return [When] called " () {
        
    }

}