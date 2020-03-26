import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXScmCheckoutSpec extends JenkinsPipelineSpecification {

    def edgeXScmCheckout = null

    def setup() {

        edgeXScmCheckout = loadPipelineScriptForTest('vars/edgeXScmCheckout.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('checkout')
        explicitlyMockPipelineStep('edgeXSetupEnvironment')
    }

    def "Test edgeXScmCheckout [Should] call checkout with expected parameters [When] config includes tags gitCheckoutExtensions" () {
        setup:
            explicitlyMockPipelineVariable('scm')
        when:
            edgeXScmCheckout({tags = true})
        then:
            1 * getPipelineMock('checkout').call(['$class':'GitSCM', 'branches':null, 'doGenerateSubmoduleConfigurations':null, 'extensions':[['$class':'CloneOption', 'noTags':false, 'shallow':false, 'depth':0, 'reference':'']], 'userRemoteConfigs':null])
    }

    def "Test edgeXScmCheckout [Should] call checkout with expected parameters [When] config includes lfs gitCheckoutExtensions" () {
        setup:
            explicitlyMockPipelineVariable('scm')
        when:
            edgeXScmCheckout({lfs = true})
        then:
            1 * getPipelineMock("checkout").call(['$class':'GitSCM', 'branches':null, 'doGenerateSubmoduleConfigurations':null, 'extensions':[['$class':'GitLFSPull']], 'userRemoteConfigs':null])
    }

    //TODO: figure out how to test the result from calling edgeXScmCheckout vars

}
