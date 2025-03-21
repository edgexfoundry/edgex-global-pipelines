import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXBuildMultiArchSpec extends JenkinsPipelineSpecification {

    def edgeXBuildMultiArch = null
    def latestImage = 'nexus3.edgexfoundry.org:10004/device-mock:latest'
    def mainImage = 'nexus3.edgexfoundry.org:10004/device-mock:main'
    
    def setup() {
        edgeXBuildMultiArch = loadPipelineScriptForTest('vars/edgeXBuildMultiArch.groovy')

        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXInfraPublish')
    }

    def "Test call [Should] raise error [When] config has no images" () {
            when:
                edgeXBuildMultiArch([
                    settingsFile: 'settingsFile'
                ])
            then:
                1 * getPipelineMock('error').call('[edgeXBuildMultiArch] Images list (images) is required.')
    }

    def "Test call [Should] raise error [When] config has no settingsFile" () {
            when:
                edgeXBuildMultiArch([
                    images: [latestImage, mainImage]
                ])
            then:
                1 * getPipelineMock('error').call('[edgeXBuildMultiArch] Project Settings File id (settingsFile) is required.')
    }

    def "Test call [Should] retag and create image manifest [When] called" () {
        setup:
            explicitlyMockPipelineVariable('edgeXDockerLogin')
            explicitlyMockPipelineVariable('bootstrapBuildX')
        when:
            edgeXBuildMultiArch([
                settingsFile: 'settingsFile',
                images: [latestImage, mainImage]
            ])
        then:
            1 * getPipelineMock('sh').call({ it.contains("echo -e 'FROM ${latestImage}' | docker buildx build --platform 'linux/amd64,linux/arm64' -t ${latestImage} --push -") })
            1 * getPipelineMock('sh').call({ it.contains("echo -e 'FROM ${mainImage}' | docker buildx build --platform 'linux/amd64,linux/arm64' -t ${mainImage} --push -") })
    }
}