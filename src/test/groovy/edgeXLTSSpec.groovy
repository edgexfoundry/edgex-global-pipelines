import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXLTSSpec extends JenkinsPipelineSpecification {

    def edgeXLTS = null

    def setup() {
        edgeXLTS = loadPipelineScriptForTest('vars/edgeXLTS.groovy')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXReleaseGitTag')
        explicitlyMockPipelineVariable('edgeXReleaseGitTagUtil')
        getPipelineMock('edgeXReleaseGitTagUtil.validate').call(_) >> true
    }

    def "Test prepLTS [Should] clone proper repo [When] expected" () {
        setup:
            def releaseInfo =
                [
                    lts:true,
                    name:'sample-service',
                    version:'1.11.83',
                    releaseName:'jakarta',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service',
                            'docker.io/edgexfoundry/sample-service'
                        ]
                    ], [
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            edgeXLTS.prepLTS(releaseInfo, [credentials: "edgex-jenkins-ssh"])
        then:
            1 * getPipelineMock("edgeXReleaseGitTag.cloneRepo").call(
                'https://github.com/edgexfoundry/sample-service.git',
                'main',
                'jakarta',
                '0123456789',
                'edgex-jenkins-ssh'
            )
    }
    
    def "Test prepGoProject [Should] perform 'make vendor' [When] called" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('docker.image')('golang') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXLTS.prepGoProject("jakarta")
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_)
            1 * getPipelineMock("sh").call("make vendor")
    }
}
