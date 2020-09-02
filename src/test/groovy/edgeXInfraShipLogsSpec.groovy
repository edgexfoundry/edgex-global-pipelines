import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraShipLogsSpec extends JenkinsPipelineSpecification {

    def edgeXInfraShipLogs = null

    def setup() {

        edgeXInfraShipLogs = loadPipelineScriptForTest('vars/edgeXInfraShipLogs.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXInfraShipLogs [Should] call expected shell scripts with expected arguments [When] called" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'ghprbPullId': true, 
                'LOGS_SERVER': 'MyLogServer',
                'SILO': 'MySilo',
                'JENKINS_HOSTNAME': 'MyJenkinsHostname',
                'JOB_NAME': 'MyJobName',
                'BUILD_NUMBER': 'MyBuildNumber'
            ]
            edgeXInfraShipLogs.getBinding().setVariable('env', environmentVariables)

            getPipelineMock("libraryResource")('global-jjb-shell/create-netrc.sh') >> {
                return 'create-netrc'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/logs-deploy.sh') >> {
                return 'logs-deploy'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/logs-clear-credentials.sh') >> {
                return 'logs-clear-credentials'
            }
            edgeXInfraShipLogs.getBinding().setVariable('currentBuild', [:])
        when:
            edgeXInfraShipLogs()
        then:
            1 * getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-lftools-log-publisher:alpine') >> explicitlyMockPipelineVariable('DockerImageMock')
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '--privileged -u 0:0 -v /var/log/sa:/var/log/sa-host'
                assert dockerArgs == _arguments[0][0]
            }
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SERVER_ID=logs'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('mkdir -p /var/log/sa')
            1 * getPipelineMock('sh').call('for file in `ls /var/log/sa-host`; do sadf -c /var/log/sa-host/${file} > /var/log/sa/${file}; done')
            1 * getPipelineMock('sh').call([script:'create-netrc'])
            1 * getPipelineMock('sh').call([script:'logs-deploy'])
            1 * getPipelineMock('sh').call([script:'logs-clear-credentials'])
            // TODO: figure out how to check value of currentBuild.description mock variable
    }
}
