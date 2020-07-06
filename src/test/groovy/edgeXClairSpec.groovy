import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXClairSpec extends JenkinsPipelineSpecification {

    def edgeXClair = null

    def setup() {
        edgeXClair = loadPipelineScriptForTest('vars/edgeXClair.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXClair [Should] throw exception [When] no image is provided" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            edgeXClair(null)
        then:
            thrown Exception
    }

    def "Test edgeXClair [Should] write Clair report with expected html and archiveArtifacts [When] called" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY': 'MyDockerRegistry'
            ]
            edgeXClair.getBinding().setVariable('env', environmentVariables)
            edgeXClair.getBinding().setVariable('WORKSPACE', 'MyWorkspace')

            // TODO: figure out how to mock other methods
            // getPipelineMock('scan')('MyDockerImage:MyTag', 'clair-alb-414217221.us-west-2.elb.amazonaws.com:6060', 'MyDockerRegistry:10003/edgex-klar:latest', 'json') >> 'KlarJsonResults'
            // getPipelineMock('scan')('MyDockerImage:MyTag', 'clair-alb-414217221.us-west-2.elb.amazonaws.com:6060', 'MyDockerRegistry:10003/edgex-klar:latest', 'html') >> 'KlarHtmlResults'

            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('readJSON')
            getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-devops/edgex-klar:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('sh').call([script:'/klar MyDockerImage:MyTag | tee', returnStdout:true]) >> 'KlarOutput\n'
            getPipelineMock('readJSON').call([text:'KlarOutput']) >> 'KlarJsonOutput'

            explicitlyMockPipelineStep('writeFile')

        when:
            edgeXClair('MyDockerImage:MyTag')
        then:
            noExceptionThrown()
            1 * getPipelineMock('archiveArtifacts.call').call(['allowEmptyArchive':true, 'artifacts':'clair-reports/clair_results_MyDockerImage_MyTag.html'])
            1 * getPipelineMock('writeFile').call(['file':'./clair-reports/clair_results_MyDockerImage_MyTag.html', 'text': """
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
    integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
  <style>
    body { padding: 10px; }
  </style>
  <title>Clair Scan Results for Image [MyDockerImage:MyTag]</title>
</head>
<body>
    <h1>Scan Results for Docker Image [MyDockerImage:MyTag]</h1>
    <pre>
KlarOutput
    </pre>
</body>
</html>
"""])
    }

    def "Test scan [Should] call expected commands with expected arguments [When] called with json outputFormat" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('readJSON')
            getPipelineMock('docker.image')('MyKlarImage') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXClair.scan('MyImage', 'MyServer', 'MyKlarImage', 'json')
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '--entrypoint='
                assert dockerArgs == _arguments[0][0]
            }
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'CLAIR_ADDR=MyServer',
                    'FORMAT_OUTPUT=json'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call([script:'/klar MyImage | tee', returnStdout:true]) >> 'KlarOutput\n'
            1 * getPipelineMock('readJSON').call([text:'KlarOutput'])
    }

    def "Test scan [Should] echo expected commands with expected arguments [When] called when DRY_RUN is true" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('readJSON')
            getPipelineMock('docker.image')('MyKlarImage') >> explicitlyMockPipelineVariable('DockerImageMock')

            def environmentVariables = [
                'DRY_RUN': 'true'
            ]
            edgeXClair.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXClair.scan('MyImage', 'MyServer', 'MyKlarImage', 'json')
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '--entrypoint='
                assert dockerArgs == _arguments[0][0]
            }
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'CLAIR_ADDR=MyServer',
                    'FORMAT_OUTPUT=json'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock("sh").call('echo /klar MyImage')
    }

    def "Test scan [Should] return expected result [When] called with json outputFormat and readJSON throws an exception" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            explicitlyMockPipelineStep('readJSON')
            getPipelineMock('docker.image')('MyKlarImage') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('sh').call([script:'/klar MyImage | tee', returnStdout:true]) >> 'KlarOutput\n'
            getPipelineMock('readJSON').call([text:'KlarOutput']) >> {
                throw new Exception('An exception occurred with readJSON')
            }
        expect:
            edgeXClair.scan('MyImage', 'MyServer', 'MyKlarImage', 'json') == [ LayerCount: 0, Vulnerabilities: [:]]
    }

    def "Test scan [Should] return expected result [When] called with json outputFormat and klar command returns nothing" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            getPipelineMock('docker.image')('MyKlarImage') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('sh').call([script:'/klar MyImage | tee', returnStdout:true]) >> '\n'
        expect:
            edgeXClair.scan('MyImage', 'MyServer', 'MyKlarImage', 'json') == [ LayerCount: 0, Vulnerabilities: [:]]
    }

    def "Test scan [Should] return expected result [When] called with non-json outputFormat" () {
        setup:
            explicitlyMockPipelineStep('withEnv')
            getPipelineMock('docker.image')('MyKlarImage') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('sh').call([script:'/klar MyImage | tee', returnStdout:true]) >> 'KlarOutput\n'
        expect:
            edgeXClair.scan('MyImage', 'MyServer', 'MyKlarImage', 'non-json') == 'KlarOutput'
    }

    def "Test getImageName [Should] return #expectedResult [When] called with #image" () {
        setup:
        expect:
            edgeXClair.getImageName(image) == expectedResult
        where:
            image << [
                'image1',
                '/dir1/dir2/dir3/image2'
            ]
            expectedResult << [
                'image1',
                'image2'
            ]
    }

    def "Test tableHtml [Should] return expected [When] called" () {
        setup:
        expect:
            def expectedResult = """
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
    integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
  <style>
    body { padding: 10px; }
  </style>
  <title>Clair Scan Results for Image [MyImage]</title>
</head>
<body>
    <h1>Scan Results for Docker Image [MyImage]</h1>
    <pre>
MyTableContent
    </pre>
</body>
</html>
"""
            edgeXClair.tableHtml('MyImage', 'MyTableContent') == expectedResult
    }

}
