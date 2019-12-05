import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraPublishSpec extends JenkinsPipelineSpecification {

    def edgeXInfraPublish = null

    def setup() {

        edgeXInfraPublish = loadPipelineScriptForTest('vars/edgeXInfraPublish.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXInfraPublish [Should] throw exception [When] logSettingsFile is null" () {
        setup:
        when:
            edgeXInfraPublish({logSettingsFile = null})
        then:
            thrown Exception
    }

    def "Test edgeXInfraPublish [Should] call expected shell scripts with expected arguments [When] called" () {
        setup:
            explicitlyMockPipelineStep('cleanWs')
            getPipelineMock("libraryResource")('global-jjb-shell/sysstat.sh') >> {
                return 'sysstat'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/package-listing.sh') >> {
                return 'package-listing'
            }
        when:
            edgeXInfraPublish()
        then:
            1 * getPipelineMock('sh').call([script:'sysstat'])
            1 * getPipelineMock('sh').call([script:'package-listing'])
    }

}
