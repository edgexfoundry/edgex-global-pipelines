import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXCodecovSpec extends JenkinsPipelineSpecification {

    def edgeXCodecov = null

    def setup() {

        edgeXCodecov = loadPipelineScriptForTest('vars/edgeXCodecov.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXCodecov [Should] call Codecov token shell script with expected arguments [When] no tokenFile is provided" () {
        setup:
                def environmentVariables = [
                'PROJECT': 'MyProject'
            ]
            edgeXCodecov.getBinding().setVariable('env', environmentVariables)
            edgeXCodecov.getBinding().setVariable('CODECOV_TOKEN', 'MyCodecovToken')
        when:
            edgeXCodecov()
        then:
            1 * getPipelineMock('configFile.call').call([fileId: 'MyProject-codecov-token', variable: 'CODECOV_TOKEN'])
            1 * getPipelineMock('sh').call('curl -s https://codecov.io/bash | bash -s - -t @MyCodecovToken')
    }

}
