import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildGoModSpec extends JenkinsPipelineSpecification {

    def edgeXBuildGoMod = null

    def setup() {
        edgeXBuildGoMod = loadPipelineScriptForTest('vars/edgeXBuildGoMod.groovy')

        explicitlyMockPipelineVariable('edgeXBuildGoApp')
    }

    def "Test edgeXBuildGoMod [Should] call edgeXBuildGoApp with expected arguments [When] called with config" () {
        when:
            edgeXBuildGoMod([
                project: 'go-mod-bootstrap',
                goVersion: '1.11'])
        then:
            1 * getPipelineMock('edgeXBuildGoApp.call')([
                project:'go-mod-bootstrap',
                goVersion:'1.11',
                buildImage:false,
                pushImage:false])
    }

}