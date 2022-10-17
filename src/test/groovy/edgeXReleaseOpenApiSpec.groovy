import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseOpenApiSpec extends JenkinsPipelineSpecification {
    def edgeXReleaseOpenApi
    def validReleaseYaml

    def setup() {
        edgeXReleaseOpenApi = loadPipelineScriptForTest('vars/edgeXReleaseOpenApi.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXReleaseGitTag')

        validReleaseYaml = [
            name: 'device-sdk-go',
            version: '2.2.0',
            releaseName: 'currentRelease',
            releaseStream: 'main',
            repo: 'https://github.com/edgexfoundry/device-sdk-go.git',
            commitId: 'c0818f6da75fef2ffe509345f5fc87075bcd5114',
            gitTag: true,
            dockerImages: false,
            gitHubRelease: false,
            apiInfo: [
                nextReleaseVersion: '2.3.0',
                reviewers: 'mock-reviewers'
            ]
        ]
    }

    def "Test call [Should] execute expected [When] openapi/v2 folder DOES exists" () {
        setup:
            getPipelineMock('fileExists').call(_) >> true
        when:
            edgeXReleaseOpenApi(validReleaseYaml)
        then:
            1 * getPipelineMock('echo').call('[edgeXReleaseOpenApi] Detected openapi/v2 folder. Validating release yaml.')
            1 * getPipelineMock('sh').call('git config --global user.email "jenkins@edgexfoundry.org"')
            1 * getPipelineMock('sh').call('git config --global user.name "EdgeX Jenkins"')
    }

    def "Test call [Should] skip execution [When] openapi/v2 folder DOES NOT exists" () {
        setup:
            getPipelineMock('fileExists').call(_) >> false
        when:
            edgeXReleaseOpenApi(validReleaseYaml)
        then:
            1 * getPipelineMock('echo').call('[edgeXReleaseOpenApi] No OpenApi Yaml to bump. Doing nothing.')
    }

    def "Test publishOpenApiChanges [Should] execute expected [When] called" () {
        when:
            edgeXReleaseOpenApi.publishOpenApiChanges(validReleaseYaml)
        then:
            1 * getPipelineMock('sh').call('git reset --hard c0818f6da75fef2ffe509345f5fc87075bcd5114')
            1 * getPipelineMock('sh').call('git checkout -b currentRelease-openapi-version-changes')
            1 * getPipelineMock('sh').call("sed -E -i 's|  version: (.*)|  version: 2.3.0|g' openapi/v2/*.yaml")
            1 * getPipelineMock('sh').call('git diff')
            1 * getPipelineMock("edgex.createPR").call(['currentRelease-openapi-version-changes', 'ci: automated version changes for OpenAPI version: [2.3.0]', 'This PR updates the OpenAPI version yaml the next release version 2.3.0', 'mock-reviewers'])
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a apiInfo attribute" () {
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.apiInfo = null
            edgeXReleaseOpenApi.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseOpenApi] Release yaml does not contain 'apiInfo' block. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a nextReleaseVersion attribute" () {
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.apiInfo = validReleaseYaml.apiInfo.findAll { it.key != 'nextReleaseVersion' }
            edgeXReleaseOpenApi.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseOpenApi] Release yaml does not contain 'nextReleaseVersion'. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a reviewers attribute" () {
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.apiInfo = validReleaseYaml.apiInfo.findAll { it.key != 'reviewers' }
            edgeXReleaseOpenApi.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call("[edgeXReleaseOpenApi] Release yaml does not contain 'reviewers'. Example: apiInfo: [ nextReleaseVersion: '1.2.3', reviewers: edgexfoundry/team-name ]")
    }

}