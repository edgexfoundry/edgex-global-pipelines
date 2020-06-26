import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSwaggerPublishSpec extends JenkinsPipelineSpecification {

    def edgeXSwaggerPublish = null

    def setup() {
        edgeXSwaggerPublish = loadPipelineScriptForTest('vars/edgeXSwaggerPublish.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('error')
        explicitlyMockPipelineVariable('writeFile')
        edgeXSwaggerPublish.getBinding().setVariable('edgex', {})
        explicitlyMockPipelineStep('isDryRun')
        explicitlyMockPipelineStep('withEnv')
    }

    def "Test edgeXSwaggerPublish [Should] call shell script with expected arguments [When] no owner is provided" () {
        when:
            edgeXSwaggerPublish()
        then:
            1 * getPipelineMock('sh').call('./edgex-publish-swagger.sh EdgeXFoundry1')
    }

    def "Test edgeXSwaggerPublish [Should] call shell script with expected arguments [When] owner is provided" () {
        when:
            edgeXSwaggerPublish('Moby')
        then:
            1 * getPipelineMock('sh').call('./edgex-publish-swagger.sh Moby')
    }
    
    def "Test retreieveResourceScripts [Should] retrieve files from global pipeline library [When] files are provided" () {
        when:
            edgeXSwaggerPublish.retreieveResourceScripts(["toSwaggerHub.sh", "edgex-publish-swagger.sh"])
        then:
            1 * getPipelineMock('libraryResource').call('toSwaggerHub.sh')
            1 * getPipelineMock('libraryResource').call('edgex-publish-swagger.sh')
            1 * getPipelineMock('writeFile.call').call(['file':'./toSwaggerHub.sh', 'text':null])
            1 * getPipelineMock('writeFile.call').call(['file':'./edgex-publish-swagger.sh', 'text':null])
    }

    def "Test edgeXSwaggerPublish [Should] should execute shell script correctly [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('isDryRun')() >> true
            explicitlyMockPipelineStep('withEnv')
            def environmentVariables = ['DRY_RUN': 'true']
            edgeXSwaggerPublish.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSwaggerPublish()
        then:
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SWAGGER_DRY_RUN=true'
                ]
                assert envArgs == _arguments[0][0]
            }
    }

}
