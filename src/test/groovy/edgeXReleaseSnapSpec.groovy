import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseSnapSpec extends JenkinsPipelineSpecification {
    
    def edgeXReleaseSnap = null
    def edgeX = null
    def validReleaseInfo
    def validSnapInfo

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXReleaseSnap = loadPipelineScriptForTest('vars/edgeXReleaseSnap.groovy')
        edgeXReleaseSnap.getBinding().setVariable('edgex', {})
        explicitlyMockPipelineStep('isDryRun')
        explicitlyMockPipelineVariable('out')
        validReleaseInfo = [
            'name': 'sample-service',
            'version': '1.2.3',
            'snap': true,
            'repo': 'https://github.com/edgexfoundry/sample-service.git',
            'releaseStream': 'master',
            'snapChannels': [
                [
                    channel: 'latest/stable',
                    revisionNumber: null
                ], [
                    channel: 'geneva/stable',
                    revisionNumber: null
                ]
            ]
        ]
        validSnapInfo = [
            "channel-map": [
                [
                    channel: [
                        architecture:"amd64",
                        name:"edge",
                        "released-at":"2020-04-16T00:14:48.629377+00:00",
                        risk:"edge",
                        track:"latest"
                    ],
                    revision:"2211"
                ], [
                    channel: [
                        architecture:"arm64",
                        name:"edge",
                        "released-at":"2020-04-16T00:14:48.629377+00:00",
                        risk:"edge",
                        track:"latest"
                    ],
                    revision:"2188"
                ], [
                    channel: [
                        architecture:"amd64",
                        name:"candidate",
                        "released-at":"2020-04-16T00:14:48.629377+00:00",
                        risk:"candidate",
                        track:"latest"
                    ],
                    revision:"2049"
                ]
            ]
        ]
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a name attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseSnap.validate(validReleaseInfo.findAll {it.key != 'name'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: Release yaml does not contain \'name\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseSnap.validate(validReleaseInfo.findAll {it.key != 'version'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: Release yaml does not contain \'version\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a snapChannels attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseSnap.validate(validReleaseInfo.findAll {it.key != 'snapChannels'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: Release yaml does not contain \'snapChannels\'')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a releaseStream attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseSnap.validate(validReleaseInfo.findAll {it.key != 'releaseStream'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: Release yaml does not contain \'releaseStream\'')
    }    

    def "Test validate [Should] raise error [When] release info yaml does not have a repo attribute" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXReleaseSnap.validate(validReleaseInfo.findAll {it.key != 'repo'})
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: Release yaml does not contain \'repo\'')
    }

    def "Test getSnapcraftAddress [Should] return #expectedResult [When] when called" () {
        setup:
        expect:
            edgeXReleaseSnap.getSnapcraftAddress(repo, branch) == expectedResult
        where:
            repo << [
                'https://github.com/edgexfoundry/sample-service.git',
                'git@github.com:edgexfoundry/sample-service.git',
                'https://github.com/edgexfoundry/edgex-go',
                'https://github.com/edgexfoundry/device-sdk-go.git',
                'https://github.com/edgexfoundry/app-functions-sdk-go.git'
            ]
            branch << [
                'master',
                'fuji',
                'master',
                'branch1',
                'branch2'
            ]
            expectedResult << [
                'https://raw.githubusercontent.com/edgexfoundry/sample-service/master/snap/snapcraft.yaml',
                'git@github.com:edgexfoundry/sample-service/fuji/snap/snapcraft.yaml',
                'https://raw.githubusercontent.com/edgexfoundry/edgex-go/master/snap/snapcraft.yaml',
                'https://raw.githubusercontent.com/edgexfoundry/device-sdk-go/branch1/snap/snapcraft.yaml',
                'https://raw.githubusercontent.com/edgexfoundry/app-functions-sdk-go/branch2/snap/snapcraft.yaml'
            ]
    }

    def "Test getSnapMetadata [Should] call sh and readYaml with the expected arguments [When] called" () {
        setup:
            def environmentVariables = [
                'WORKSPACE': '/w/thecars'
            ]
            edgeXReleaseSnap.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXReleaseSnap.getSnapMetadata("https://github.com/edgexfoundry/edgex-go", "master", "edgex-go")
        then:
            1 * getPipelineMock("sh").call("curl --fail -o /w/thecars/snapcraft-edgex-go.yaml -O https://raw.githubusercontent.com/edgexfoundry/edgex-go/master/snap/snapcraft.yaml")
            1 * getPipelineMock("readYaml").call(file: "/w/thecars/snapcraft-edgex-go.yaml")
    }

    def "Test getSnapInfo [Should] call sh with the expected arguments [When] called" () {
        setup:
        when:
            edgeXReleaseSnap.getSnapInfo("edgexfoundry")
        then:
            1 * getPipelineMock("sh").call([
                    script: "curl --fail -H 'Snap-Device-Series: 16' 'https://api.snapcraft.io/v2/snaps/info/edgexfoundry?fields=name,revision'",
                    returnStdout: true]) >> '{}'
    }

    def "Test getSnapRevision [Should] return expected revision [When] called" () {
        setup:
        expect:
            expectedResult == edgeXReleaseSnap.getSnapRevision(validSnapInfo, architecture, trackName)
        where:
            trackName << [
                'latest/edge',
                'latest/edge',
                'latest/candidate',
                'latest/candidate'

            ]
            architecture << [
                'amd64',
                'arm64',
                'amd64',
                'arm64'
            ]
            expectedResult << [
                '2211',
                '2188',
                '2049',
                '1'
            ]
    }

    def "Test releaseSnap [Should] catch and raise error [When] exception occurs" () {
        setup:
            explicitlyMockPipelineStep('error')
            // TODO: figure out how to properly stub getSnapMetadata to set side-effect Exception
            // explicitlyMockPipelineStep('getSnapMetadata')
            // getPipelineMock('getSnapMetadata').call(_) >> {
            //     throw new Exception('Get Snap Metadata Exception')
            // }
            def environmentVariables = [
                'WORKSPACE': '/w/thecars'
            ]
            edgeXReleaseSnap.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh').call('curl --fail -o /w/thecars/snapcraft-sample-service.yaml -O https://raw.githubusercontent.com/edgexfoundry/sample-service/master/snap/snapcraft.yaml') >> {
                throw new Exception('curl Exception')
            }
        when:
            edgeXReleaseSnap.releaseSnap(validReleaseInfo)
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseSnap]: ERROR occurred releasing snap: java.lang.Exception: curl Exception')
    }

    def "Test releaseSnap [Should] call expected [When] DRY_RUN is true" () {
        setup:
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineStep('edgeXSnap')
            explicitlyMockPipelineStep('withEnv')
            def environmentVariables = [
                'WORKSPACE': '/w/thecars',
                'DRY_RUN': 'true'
            ]
            def archList = []
            edgeXReleaseSnap.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('isDryRun')() >> true
            getPipelineMock('readYaml').call(file: '/w/thecars/snapcraft-sample-service.yaml') >> [
                name: 'sample-service',
                architectures: [
                    [
                        'build-on':'arm64'
                    ], [
                        'build-on':'amd64'
                    ], [
                        'build-on':'armhf'
                    ]
                ]
            ]
            getPipelineMock('sh').call([
                script: "curl --fail -H 'Snap-Device-Series: 16' 'https://api.snapcraft.io/v2/snaps/info/sample-service?fields=name,revision'",
                returnStdout: true]) >> ''
            getPipelineMock('readJSON').call(text: _) >> validSnapInfo
        when:
            edgeXReleaseSnap.releaseSnap(validReleaseInfo)
        then:
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: latest/stable, snapRevision: 2211, snapName: sample-service) - DRY_RUN: true")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: latest/stable, snapRevision: 2188, snapName: sample-service) - DRY_RUN: true")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: geneva/stable, snapRevision: 2211, snapName: sample-service) - DRY_RUN: true")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: geneva/stable, snapRevision: 2188, snapName: sample-service) - DRY_RUN: true")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: architecture armhf is not supported")
            0 * getPipelineMock('edgeXSnap').call(_)
            4 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                    archList << _arguments[0][0][0]
                }
            assert archList == ['ARCH=amd64', 'ARCH=amd64', 'ARCH=amd64', 'ARCH=amd64']
            // TODO: figure out why the assertion below didn't work
            // 2 * getPipelineMock('withEnv').call(_) >> { _arguments ->
            //         def envArgs = [
            //             'ARCH=arm64'
            //         ]
            //         assert envArgs == _arguments[0][0]
            //     }
    }

    def "Test releaseSnap [Should] call expected [When] DRY_RUN is false" () {
        setup:
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineStep('edgeXSnap')
            explicitlyMockPipelineStep('withEnv')
            def environmentVariables = [
                'WORKSPACE': '/w/thecars',
                'DRY_RUN': 'false'
            ]
            def archList = []
            edgeXReleaseSnap.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('isDryRun')() >> false
            getPipelineMock('readYaml').call(file: '/w/thecars/snapcraft-sample-service.yaml') >> [
                name: 'sample-service',
                architectures: [
                    [
                        'build-on':'arm64'
                    ], [
                        'build-on':'amd64'
                    ], [
                        'build-on':'armhf'
                    ]
                ]
            ]
            getPipelineMock('sh').call([
                script: "curl --fail -H 'Snap-Device-Series: 16' 'https://api.snapcraft.io/v2/snaps/info/sample-service?fields=name,revision'",
                returnStdout: true]) >> ''
            getPipelineMock('readJSON').call(text: _) >> validSnapInfo
            // URGH: was really hoping this would work based off how parseJson was mocked
            // explicitlyMockPipelineStep('getSnapInfo')
            // getPipelineMock('getSnapInfo')('sample-service') >> validSnapInfo
        when:
            edgeXReleaseSnap.releaseSnap(validReleaseInfo)
        then:
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: architecture armhf is not supported")
            1 * getPipelineMock('edgeXSnap').call([jobType: 'release', snapChannel: 'latest/stable', snapRevision: '2211', snapName: 'sample-service'])
            1 * getPipelineMock('edgeXSnap').call([jobType: 'release', snapChannel: 'latest/stable', snapRevision: '2188', snapName: 'sample-service'])
            1 * getPipelineMock('edgeXSnap').call([jobType: 'release', snapChannel: 'geneva/stable', snapRevision: '2211', snapName: 'sample-service'])
            1 * getPipelineMock('edgeXSnap').call([jobType: 'release', snapChannel: 'geneva/stable', snapRevision: '2188', snapName: 'sample-service'])
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: latest/stable, snapRevision: 2211, snapName: sample-service) - DRY_RUN: false")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: latest/stable, snapRevision: 2188, snapName: sample-service) - DRY_RUN: false")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: geneva/stable, snapRevision: 2211, snapName: sample-service) - DRY_RUN: false")
            1 * getPipelineMock('echo').call("[edgeXReleaseSnap]: edgeXSnap(jobType: 'release', snapChannel: geneva/stable, snapRevision: 2188, snapName: sample-service) - DRY_RUN: false")
            4 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                    archList << _arguments[0][0][0]
                }
            assert archList == ['ARCH=amd64', 'ARCH=amd64', 'ARCH=amd64', 'ARCH=amd64']
    }

    def "Test edgeXReleaseSnap [Should] echo repo is not snap enabled [When] release info yaml snap is false" () {
        setup:
            explicitlyMockPipelineStep('echo')
        when:
            validReleaseInfo['snap'] = false
            edgeXReleaseSnap(validReleaseInfo)
            validReleaseInfo['snap'] = true
        then:
            1 * getPipelineMock('echo').call('[edgeXReleaseSnap]: repo is not snap enabled')
    }

}