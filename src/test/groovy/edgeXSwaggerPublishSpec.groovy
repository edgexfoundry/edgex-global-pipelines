import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSwaggerPublishSpec extends JenkinsPipelineSpecification {

    def edgeXSwaggerPublish = null

    def setup() {
        edgeXSwaggerPublish = loadPipelineScriptForTest('vars/edgeXSwaggerPublish.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
    }

    def "Test edgeXSwaggerPublish [Should] should fail [When] no API Folder paths are provided" () {
        setup:
        when:
            edgeXSwaggerPublish()
        then:
            1 * getPipelineMock('error').call('[edgeXSwaggerPublish]: No list of API Folders given')
    }

    def "Test edgeXSwaggerPublish [Should] call shell script with expected arguments [When] no owner is provided" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXSwaggerPublish(apiFolders:'api/v1')
        then:
            1 * getPipelineMock("libraryResource").call('edgex-publish-swagger.sh')
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SWAGGER_DRY_RUN=true',
                    'OWNER=EdgeXFoundry1',
                    'API_FOLDERS=api/v1'
                ]
                assert envArgs == _arguments[0][0]
            }
    }

    def "Test edgeXSwaggerPublish [Should] call shell script with expected arguments [When] owner is provided" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXSwaggerPublish(owner: 'Moby', apiFolders:'openapi/v1 openapi/v2')
        then:
            1 * getPipelineMock("libraryResource").call('edgex-publish-swagger.sh')
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SWAGGER_DRY_RUN=false',
                    'OWNER=Moby',
                    'API_FOLDERS=openapi/v1 openapi/v2'
                ]
                assert envArgs == _arguments[0][0]
            }
    }

    def "Test edgeXSwaggerPublish [Should] should execute shell script correctly [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXSwaggerPublish(apiFolders: 'openapi/v1 openapi/v2 custom_api')
        then:
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SWAGGER_DRY_RUN=true',
                    'OWNER=EdgeXFoundry1',
                    'API_FOLDERS=openapi/v1 openapi/v2 custom_api'
                ]
                assert envArgs == _arguments[0][0]
            }
    }

}
