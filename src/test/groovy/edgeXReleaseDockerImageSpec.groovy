import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDockerImageSpec extends JenkinsPipelineSpecification {
    def edgeXReleaseDockerImage
    def validReleaseYaml

    def setup() {
        edgeXReleaseDockerImage = loadPipelineScriptForTest('vars/edgeXReleaseDockerImage.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXDocker')

        validReleaseYaml = [
            name:'app-functions-sdk-go',
            version:'v1.2.0',
            releaseName:'geneva',
            releaseStream:'main',
            repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
            dockerImages:true,
            docker:[[
                image: 'nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go',
                destination: [
                    'nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go',
                    'edgexfoundry/docker-app-functions-sdk-go'
                ]
            ], [
                image: 'nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go-arm64',
                destination: [
                    'nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go-arm64',
                    'edgexfoundry/docker-app-functions-sdk-go-arm64'
                ]
            ]]
        ]
    }

    def "Test getAvaliableTargets [Should] return expected [When] called " () {
        setup:
        expect:
            edgeXReleaseDockerImage.getAvaliableTargets() == expectedResult
        where:
            expectedResult = [
                'nexus3.edgexfoundry.org:10002': 'release',
                'docker.io': 'dockerhub'
            ]
    }

    def "Test isValidReleaseRegistry [Should] return valid docker release target [When] called " () {
        setup:
        expect:
            [
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'docker.io', namespace: 'edgexfoundry', image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'docker.io', namespace: null, image: 'python', tag: '3-alpine']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10002', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10004', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'nexus3.edgexfoundry.org:10003', namespace: null, image: 'sample-service']),
                edgeXReleaseDockerImage.isValidReleaseRegistry([host: 'example.com', namespace: null, image: 'sample-service'])
            ] == expectedResult
        where:
            expectedResult = [
                true,
                false,
                true,
                false,
                false,
                false
            ]
    }

    def "Test publishDockerImage [Should] call expected [When] DRY_RUN and DRY_RUN_PULL_DOCKER_IMAGES are true" () {
        setup:
            getPipelineMock('edgeXDocker.toImageStr').call('MyFrom') >> 'MyFromImageStr'
            getPipelineMock('edgeXDocker.toImageStr').call('MyTo') >> 'MyToImageStr'
            getPipelineMock('edgex.isDryRun').call() >> true
            def environmentVariables = [
                'DRY_RUN_PULL_DOCKER_IMAGES': 'true'
            ]
            edgeXReleaseDockerImage.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXReleaseDockerImage.publishDockerImage('MyFrom', 'MyTo')
        then:
            1 * getPipelineMock('sh').call('docker pull MyFromImageStr')
            1 * getPipelineMock('echo').call([
                    'docker pull MyFromImageStr',
                    'docker tag MyFromImageStr MyToImageStr',
                    'docker push MyToImageStr'
                ].join("\n"))
    }

    def "Test publishDockerImage [Should] call expected [When] DRY_RUN is not set" () {
        setup:
            getPipelineMock('edgeXDocker.toImageStr').call('MyFrom') >> 'MyFromImageStr'
            getPipelineMock('edgeXDocker.toImageStr').call('MyTo') >> 'MyToImageStr'
            getPipelineMock('edgex.isDryRun').call() >> false
        when:
            edgeXReleaseDockerImage.publishDockerImage('MyFrom', 'MyTo')
        then:
            1 * getPipelineMock('sh').call('docker pull MyFromImageStr')
            1 * getPipelineMock('sh').call('docker tag MyFromImageStr MyToImageStr')
            1 * getPipelineMock('sh').call('docker push MyToImageStr')
    }

    def "Test validate [Should] raise error [When] release info yaml does not contain docker attribute" () {
        setup:
        when:
            edgeXReleaseDockerImage.validate(validReleaseYaml.findAll {it.key != 'docker'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDockerImage] Release yaml does not contain a list \'docker\' images')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a releaseStream attribute" () {
        setup:
        when:
            edgeXReleaseDockerImage.validate(validReleaseYaml.findAll {it.key != 'releaseStream'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDockerImage] Release yaml does not contain \'releaseStream\' (branch where you are releasing from). Example: main')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            edgeXReleaseDockerImage.validate(validReleaseYaml.findAll {it.key != 'version'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDockerImage] Release yaml does not contain release \'version\'. Example: v1.1.2')
    }

}