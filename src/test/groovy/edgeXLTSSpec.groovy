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
        explicitlyMockPipelineVariable('out')
    }

    def "Test prepLTS [Should] clone proper repo [When] expected" () {
        setup:
            getPipelineMock('sh')([ script: 'git rev-parse HEAD', returnStdout: true ]) >> {
                'new-commit-id'
            }
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

    def "Test prepLTS [Should] return new commit id [When] called" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('sh')([ script: 'git rev-parse HEAD', returnStdout: true ]) >> {
                'new-commit-id'
            }

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
        expect:
            edgeXLTS.prepLTS(releaseInfo, [credentials: "edgex-jenkins-ssh"]) == 'new-commit-id'
    }
    
    def "Test prepGoProject [Should] perform 'make vendor' [When] called" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('edgex.getGoLangBaseImage').call(_) >> 'edgex-golang-base:1.16-alpine'
            getPipelineMock('docker.image')('edgex-golang-base:1.16-alpine') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXLTS.prepGoProject("jakarta")
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_)
            1 * getPipelineMock("sh").call("make vendor")
    }

    def "Test generateLTSCommitMessage [Should] return expected #expectedResult [When] called with with version #version and commitId" () {
        setup:
        expect:
            edgeXLTS.generateLTSCommitMessage(version, commitId) == expectedResult
        where:
            version << [
                '1.0.0',
                '2.0.1',
                'MyVersion'
            ]
            commitId << [
                '68afb0ededb631de666770143fbbf223a3b7d4dd',
                '68afb0ededb631de666770143fbbf223a3b7d4dd',
                '68afb0ededb631de666770143fbbf223a3b7d4dd',
            ]
            expectedResult << [
                'ci(lts-release): LTS release v1.0.0 @68afb0e',
                'ci(lts-release): LTS release v2.0.1 @68afb0e',
                'ci(lts-release): LTS release vMyVersion @68afb0e'
            ]
    }
}
