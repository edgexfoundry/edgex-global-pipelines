import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

public class EdgeXSetupEnvironmentSpec extends JenkinsPipelineSpecification {

    def edgeXSetupEnvironment = null

    def setup() {
        edgeXSetupEnvironment = loadPipelineScriptForTest('vars/edgeXSetupEnvironment.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXSetupEnvironment [Should] set expected environment variables [When] called" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'origin/master',
                'GIT_COMMIT': '0fe050cff581bd7866a842f45ab0666d7601b761'
            ]
            edgeXSetupEnvironment.getBinding().setVariable('env', environmentVariables)

        when:
            edgeXSetupEnvironment([
                'VAR1': 'VAL1',
                'VAR2': 'VAL2'])
        then:
            environmentVariables['SEMVER_PRE_PREFIX'] == 'dev'
            environmentVariables['VAR1'] == 'VAL1'
            environmentVariables['VAR2'] == 'VAL2'
            environmentVariables['SEMVER_BRANCH'] == 'master'
            environmentVariables['GIT_BRANCH_CLEAN'] == 'origin_master'
            environmentVariables['SHORT_GIT_COMMIT'] == '0fe050c'
            environmentVariables['GIT_BRANCH'] == 'origin/master'
            environmentVariables['GIT_COMMIT'] == '0fe050cff581bd7866a842f45ab0666d7601b761'
    }
}
