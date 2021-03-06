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
                'PROJECT': 'MyProject',
                'CODECOV_TOKEN_FILE': 'MyCodecovToken'
            ]
            edgeXCodecov.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXCodecov()
        then:
            1 * getPipelineMock('configFile.call').call([fileId: 'MyProject-codecov-token', variable: 'CODECOV_TOKEN_FILE'])
            1 * getPipelineMock('sh').call('set +x ; export CODECOV_TOKEN=\$(cat MyCodecovToken) ; set -x ; curl -s https://codecov.io/bash | bash -s --')
    }

}
