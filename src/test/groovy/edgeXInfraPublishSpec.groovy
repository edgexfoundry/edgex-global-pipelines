import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraPublishSpec extends JenkinsPipelineSpecification {

    def edgeXInfraPublish = null

    def setup() {
        edgeXInfraPublish = loadPipelineScriptForTest('vars/edgeXInfraPublish.groovy')
        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('lfInfraShipLogs')

        explicitlyMockPipelineVariable('edgex')
        getPipelineMock('edgex.defaultTrue').call(null) >> true
        getPipelineMock('edgex.defaultTrue').call(true) >> true
        getPipelineMock('edgex.defaultTrue').call(false) >> false
    }

    def "Test edgeXInfraPublish [Should] call expected shell scripts with expected arguments [When] docker optimized" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'WORKSPACE': 'MyWorkspace'
            ]
            edgeXInfraPublish.getBinding().setVariable('env', environmentVariables)

            getPipelineMock("libraryResource")('global-jjb-shell/sysstat.sh') >> {
                return 'sysstat'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/package-listing.sh') >> {
                return 'package-listing'
            }
            getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-lftools-log-publisher:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXInfraPublish()
        then:
            1 * getPipelineMock('sh').call([script:'sysstat'])
            1 * getPipelineMock('sh').call([script:'package-listing'])
            1 * getPipelineMock('sh').call('facter operatingsystem > ./facter-os')
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '--privileged -u 0:0 --net host -v /var/log/sysstat:/var/log/sysstat -v /var/log/secure:/var/log/secure -v /var/log/auth.log:/var/log/auth.log -v MyWorkspace/facter-os:/facter-os -v /proc/uptime:/proc/uptime -v /run/cloud-init/result.json:/run/cloud-init/result.json'
                assert dockerArgs == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('touch /tmp/pre-build-complete')
    }

    def "Test getLogPublishContainerArgs [Should] return expected #expectedResult [When] called" () {
        setup:
            def environmentVariables = [
                'WORKSPACE': 'MyWorkspace'
            ]
            edgeXInfraPublish.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeXInfraPublish.getLogPublishContainerArgs() == expectedResult
        where:
            expectedResult = [
                '--privileged',
                '-u 0:0',
                '--net host',
                '-v /var/log/sysstat:/var/log/sysstat',
                '-v /var/log/secure:/var/log/secure',
                '-v /var/log/auth.log:/var/log/auth.log',
                '-v MyWorkspace/facter-os:/facter-os',
                '-v /proc/uptime:/proc/uptime',
                '-v /run/cloud-init/result.json:/run/cloud-init/result.json'
            ]
    }

    def "Test edgeXInfraPublish [Should] call expected shell scripts with expected arguments [When] not docker optimized" () {
        setup:
            getPipelineMock("libraryResource")('global-jjb-shell/sysstat.sh') >> {
                return 'sysstat'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/package-listing.sh') >> {
                return 'package-listing'
            }
        when:
            edgeXInfraPublish {
                dockerOptimized = false
            }
        then:
            1 * getPipelineMock('sh').call([script:'sysstat'])
            1 * getPipelineMock('sh').call([script:'package-listing'])
    }

}
