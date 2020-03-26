import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerNexusSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseDockerNexus = null

    def setup() {
        edgeXReleaseDockerNexus = loadPipelineScriptForTest('vars/edgeXReleaseDockerNexus.groovy')
        explicitlyMockPipelineVariable('out')
    }

    @Ignore
    def "Test edgeXReleaseDockerNexus [Should] return [When] called " () {
        
    }

}