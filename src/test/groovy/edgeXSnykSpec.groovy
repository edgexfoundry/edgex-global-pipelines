import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSnykSpec extends JenkinsPipelineSpecification {

    def edgeXSnyk = null
    def snykImage = 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-snyk-go:1.410.4'

    def setup() {
        edgeXSnyk = loadPipelineScriptForTest('vars/edgeXSnyk.groovy')
        explicitlyMockPipelineVariable('out')

        explicitlyMockPipelineVariable('edgex')
        getPipelineMock('edgex.defaultTrue').call(null) >> true
        getPipelineMock('edgex.defaultTrue').call(true) >> true
        getPipelineMock('edgex.defaultFalse').call(null) >> false
        getPipelineMock('edgex.defaultFalse').call(false) >> false
        getPipelineMock('edgex.defaultFalse').call(true) >> true
    }

    def "Test edgeXSnyk [Should] call snyk monitor without options [When] called with no arguments" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk()
        then:
            1 * getPipelineMock("sh").call([script: 'set -o pipefail ; snyk monitor --org=edgex-jenkins', returnStatus: true])
    }

    def "Test edgeXSnyk [Should] call snyk monitor without options [When] called with no arguments when SNYK_ORG" () {
        setup:
            def environmentVariables = [
                'SNYK_ORG': 'MySnykOrg'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk()
        then:
            1 * getPipelineMock("sh").call([script: 'set -o pipefail ; snyk monitor --org=MySnykOrg', returnStatus: true])
    }

    // Docker image tests

    def "Test edgeXSnyk [Should] provide the expected arguments to the snyk docker image [When] called" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile')
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = "-u 0:0 --privileged -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=''"
                assert dockerArgs == _arguments[0][0]
            }
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options [When] called with dockerImage and dockerFile arguments and no severity threshold" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile')
        then:
            1 * getPipelineMock("sh").call([script: 'set -o pipefail ; snyk test --org=edgex-jenkins --docker MyDockerImage --file=./MyDockerFile --policy-path=./.snyk | tee snykResults.log', returnStatus: true])
            1 * getPipelineMock("sh").call("set -o pipefail ; curl -s 'https://raw.githubusercontent.com/edgexfoundry/security-pipeline-policies/main/snyk/.snyk' | tee .snyk")
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options [When] called with dockerImage and dockerFile arguments and high severity threshold" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', severity: 'high')
        then:
            1 * getPipelineMock("sh").call([script: 'set -o pipefail ; snyk test --org=edgex-jenkins --docker MyDockerImage --file=./MyDockerFile --severity-threshold=high --policy-path=./.snyk | tee snykResults.log', returnStatus: true])
            1 * getPipelineMock("sh").call("set -o pipefail ; curl -s 'https://raw.githubusercontent.com/edgexfoundry/security-pipeline-policies/main/snyk/.snyk' | tee .snyk")
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options and send email [When] called with dockerImage and dockerFile arguments" () {
        setup:
            def environmentVariables = [
                'PROJECT': 'mock',
                'JOB_NAME': 'MyMockJob',
                'BUILD_URL': 'MockBuildUrl/'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')

            // exit code 1
            // simulate scan returning potential vulnerable results
            getPipelineMock('sh')([
                returnStatus: true,
                script: 'set -o pipefail ; snyk test --org=edgex-jenkins --docker MyDockerImage --file=./MyDockerFile --policy-path=./.snyk | tee snykResults.log'
            ]) >> {
                1
            }
            getPipelineMock('readFile').call(file: './snykResults.log') >> 'MockSnykLog'

            def messageBody = '''Possible vulnerablities have been found by Snyk in for the following build: MyMockJob
More details can be found here:
===============================================
Build URL: MockBuildUrl/
Build Console: MockBuildUrl/console

Snyk Log:
MockSnykLog
'''
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', emailTo: 'mock-user@example.com')
        then:
            1 * getPipelineMock("string.call").call(['credentialsId':'snyk-cli-token', 'variable':'SNYK_TOKEN'])
            1 * getPipelineMock("mail").call([body: messageBody, subject: '[Snyk] Possible vulnerabilities discovered in [mock] scan', to: 'mock-user@example.com'])
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options and not send email [When] called with dockerImage and dockerFile arguments" () {
        setup:
            def environmentVariables = [
                'JOB_BASE_NAME': 'MyMockJob',
                'BUILD_URL': 'MockBuildUrl/'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')

            // exit code 1
            // simulate scan returning potential vulnerable results
            getPipelineMock('sh')([
                returnStatus: true,
                script: 'set -o pipefail ; snyk test --org=edgex-jenkins --docker MyDockerImage --file=./MyDockerFile --policy-path=./.snyk | tee snykResults.log'
            ]) >> {
                1
            }
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', emailTo: 'mock-user@example.com', sendEmail: false)
        then:
            0 * getPipelineMock("mail").call()
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options and error [When] called with dockerImage and dockerFile arguments" () {
        setup:
            def environmentVariables = [
                'JOB_BASE_NAME': 'MyMockJob',
                'BUILD_URL': 'MockBuildUrl/'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')

            // exit code 2
            // simulate scan error
            getPipelineMock('sh')([
                returnStatus: true,
                script: 'set -o pipefail ; snyk test --org=edgex-jenkins --docker MyDockerImage --file=./MyDockerFile --policy-path=./.snyk | tee snykResults.log'
            ]) >> {
                2
            }
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', emailTo: 'mock-user@example.com', sendEmail: false)
        then:
            1 * getPipelineMock('error').call('[edgeXSnyk] An error occurred during the snyk scan see console output for details.')
    }

    def "Test edgeXSnyk [Should] call snyk test with docker options and send HTML email [When] called with dockerImage and dockerFile arguments" () {
        setup:
            def environmentVariables = [
                'PROJECT': 'mock',
                'SNYK_ORG': 'MockOrg'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')

            // exit code 1
            // simulate scan returning potential vulnerable results
            getPipelineMock('sh')([
                returnStatus: true,
                script: 'set -o pipefail ; snyk test --org=MockOrg --docker MyDockerImage --file=./MyDockerFile --policy-path=./.snyk --json | snyk-to-html -o snykReport.html'
            ]) >> {
                1
            }
            getPipelineMock('readFile').call(file: './snykReport.html') >> 'MockHTML'
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', emailTo: 'mock-user@example.com', htmlReport: true)
        then:
            1 * getPipelineMock("string.call").call(['credentialsId':'snyk-cli-token', 'variable':'SNYK_TOKEN'])
            1 * getPipelineMock("sh").call('rm -f snykReport.html')
            1 * getPipelineMock("mail").call([body: 'MockHTML', subject: '[Snyk] Possible vulnerabilities discovered in [mock] scan', to: 'mock-user@example.com', mimeType: 'text/html'])
    }

    def "Test edgeXSnyk [Should] download global snyk ignore policy [When] called with test" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile')
        then:
            1 * getPipelineMock("sh").call("set -o pipefail ; curl -s 'https://raw.githubusercontent.com/edgexfoundry/security-pipeline-policies/main/snyk/.snyk' | tee .snyk")
    }

    def "Test edgeXSnyk [Should] download custom snyk ignore policy [When] called with test" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'test', dockerImage: 'MyDockerImage', dockerFile: 'MyDockerFile', ignorePolicy: 'https://mock.com/.synk')
        then:
            1 * getPipelineMock("sh").call("set -o pipefail ; curl -s 'https://mock.com/.synk' | tee .snyk")
    }

    def "Test edgeXSnyk [Should] not download global snyk ignore policy [When] called with monitor" () {
        setup:
            getPipelineMock('docker.image')(snykImage) >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk(command: 'monitor')
        then:
            0 * getPipelineMock("sh").call("set -o pipefail ; curl -s 'https://raw.githubusercontent.com/edgexfoundry/security-pipeline-policies/main/snyk/.snyk' | tee .snyk")
    }

}
