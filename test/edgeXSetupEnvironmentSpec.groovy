import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore
import org.springframework.mock.env.MockEnvironment

public class EdgeXSetupEnvironmentSpec extends JenkinsPipelineSpecification {

    def edgeXSetupEnvironment = null
    def environment = [:]
    def mockEnvironment = new MockEnvironment().withProperty('GIT_BRANCH', 'origin/master')

    // mockEnvironment.setProperty('GIT_BRANCH', 'origin/master')
    // mockEnvironment.setProperty('GIT_COMMIT', '0fe050cff581bd7866a842f45ab0666d7601b761')

    def setup() {
        edgeXSetupEnvironment = loadPipelineScriptForTest('vars/edgeXSetupEnvironment.groovy')
        // edgeXSetupEnvironment.getBinding().setVariable('env', environment)
        // explicitlyMockPipelineVariable('out')
    }

    @Ignore
    def "Test call [Should] call expected [When] called" () {
        setup:
            // def environmentVariables = [
            //     'GIT_COMMIT': 'MyGitCommit'
            // ]
            // edgeXSetupEnvironment.getBinding().setVariable('env', environmentVariables)
            // getPipelineMock('env.getProperty')() >> ''
            // explicitlyMockPipelineVariable('env')
            // explicitlyMockPipelineStep('getProperty')('GIT_BRANCH') >> {
            //     'origin/master'
            // }
            // explicitlyMockPipelineStep('getProperty')('GIT_COMMIT') >> {
            //     '0fe050cff581bd7866a842f45ab0666d7601b761'
            // }
            // explicitlyMockPipelineStep('setProperty')

            // explicitlyMockPipelineVariable("GIT_BRANCH")
            // explicitlyMockPipelineVariable("GIT_COMMIT")

            // getPipelineMock('env.getProperty')('GIT_BRANCH') >> {
            //     'reyes_branch'
            // }
            // getPipelineMock('env.getProperty')('GIT_COMMIT') >> {
            //     '0fe050cff581bd7866a842f45ab0666d7601b761'
            // }
            explicitlyMockPipelineVariable('env')
            

        when:
            edgeXSetupEnvironment()

        then:
            1 * getPipelineMock('env.getProperty')('GIT_BRANCH') >> 'MyGitBranch'
            noExceptionThrown()
            // 1 * getPipelineMock('env.getProperty')('GIT_BRANCH') >> 'master'
            // 1 * getPipelineMock('setProperty').call('SEMVER_PRE_PREFIX', 'dev')
    }

}
