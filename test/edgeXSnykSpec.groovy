import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSnykSpec extends JenkinsPipelineSpecification {

    def edgeXSnyk = null

    def setup() {
        edgeXSnyk = loadPipelineScriptForTest('vars/edgeXSnyk.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXSnyk [Should] call snyk monitor without options [When] called with no arguments" () {
        setup:
            def environmentVariables = [
                'WORKSPACE': 'MyWorkspace'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-snyk-go:1.217.3') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk()
        then:
            1 * getPipelineMock('sh').call('snyk monitor')
    }

    def "Test edgeXSnyk [Should] call snyk monitor with docker options [When] called with dockerImage and dockerFile arguments" () {
        setup:
            def environmentVariables = [
                'WORKSPACE': 'MyWorkspace'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-snyk-go:1.217.3') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk('MyDockerImage', 'MyDockerFile')
        then:
            1 * getPipelineMock('sh').call('snyk monitor --docker MyDockerImage --file=./MyDockerFile')
    }

    def "Test edgeXSnyk [Should] provide the expected arguments to the snyk docker image [When] called" () {
        setup:
            def environmentVariables = [
                'WORKSPACE': 'MyWorkspace'
            ]
            edgeXSnyk.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-snyk-go:1.217.3') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXSnyk('MyDockerImage', 'MyDockerFile')
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = "-u 0:0 --privileged -v /var/run/docker.sock:/var/run/docker.sock -v MyWorkspace:/ws -w /ws --entrypoint=''"
                assert dockerArgs == _arguments[0][0]
            }
    }    

}
