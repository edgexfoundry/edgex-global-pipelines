import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSpec extends JenkinsPipelineSpecification {

    def edgeX = null
    def environment = [:]

    def setup() {
        edgeX = loadPipelineScriptForTest('vars/edgex.groovy')
        edgeX.getBinding().setVariable('env', environment)
        explicitlyMockPipelineVariable('out')
    }

    def "Test isReleaseStream [Should] return expected [When] called in production" () {
        setup:
            edgeX.getBinding().setVariable('env', [SILO: 'production'])

        expect:
            edgeX.isReleaseStream('master') == true
            edgeX.isReleaseStream('california') == true
            edgeX.isReleaseStream('delhi') == true
            edgeX.isReleaseStream('edinburgh') == true
            edgeX.isReleaseStream('fuji') == true
            edgeX.isReleaseStream('xyzmaster') == false
            edgeX.isReleaseStream('masterxyz') == false
            edgeX.isReleaseStream('xyzmasterxyz') == false
    }

    def "Test isReleaseStream [Should] return expected [When] called in non-production" () {
        setup:
            edgeX.getBinding().setVariable('env', [SILO: 'sandbox'])

        expect:
            edgeX.isReleaseStream('master') == false
            edgeX.isReleaseStream('california') == false
            edgeX.isReleaseStream('delhi') == false
            edgeX.isReleaseStream('edinburgh') == false
            edgeX.isReleaseStream('fuji') == false
            edgeX.isReleaseStream('xyzmaster') == false
            edgeX.isReleaseStream('masterxyz') == false
            edgeX.isReleaseStream('xyzmasterxyz') == false
    }

    def "Test isReleaseStream [Should] return expected [When] called without branchName in production" () {
        setup:
            edgeX.getBinding().setVariable('env', [GIT_BRANCH: 'us5375'])

        expect:
            edgeX.isReleaseStream() == expectedResult

        where:
            expectedResult << [
                false
            ]
    }

    def "Test didChange [Should] return true [When] there is no previous commit" () {
        setup:
        expect:
            edgeX.didChange('test', null) == expectedResult

        where:
            expectedResult << [
                true
            ]
    }

    def "Test didChange [Should] return false [When] previous commit on non master branch has no changes" () {
        setup:
            def environmentVariables = [
                GIT_BRANCH: 'us5375',
                GIT_COMMIT: '6c48b4195c2eda681d9817e490d6fbb8042956fc'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git diff --name-only 6c48b4195c2eda681d9817e490d6fbb8042956fc origin/master | grep \"test\" | wc -l']) >> {
                    '0'
            }

        expect:
            edgeX.didChange('test') == expectedResult

        where:
            expectedResult << [
                false
            ]
    }

    def "Test didChange [Should] return true [When] previous commit on master branch - the build is triggered manually" () {
        setup:
            def environmentVariables = [
                GIT_BRANCH: 'master',
                GIT_COMMIT: '7d3f2c0273125e8082e3acf436c918364558d8d0'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git show --pretty=%H 7d3f2c0273125e8082e3acf436c918364558d8d0^1 | xargs']) >> {
                    '7d3f2c0273125e8082e3acf436c918364558d8d0'
            }
            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git diff --name-only 7d3f2c0273125e8082e3acf436c918364558d8d0 7d3f2c0273125e8082e3acf436c918364558d8d0 | grep \"test\" | wc -l']) >> {
                    '0'
            }

        expect:
            edgeX.didChange('test') == expectedResult

        where:
            expectedResult << [
                true
            ]
    }

    def "Test mainNode [Should] return default label [When] empty nodes are passed in" () {
        setup:
        expect:
            edgeX.mainNode([nodes: [[]]]) == expectedResult
        where:
            expectedResult << [
                'centos7-docker-4c-2g'
            ]
    }

    def "Test mainNode [Should] return node label [When] list that contains a default node is passed in" () {
        setup:
        expect:
            edgeX.mainNode([nodes: [[label: 'soda stereo', isDefault: true]]]) == expectedResult
        where:
            expectedResult << [
                'soda stereo'
            ]
    }

    def "Test mainNode [Should] return expected [When] called" () {
        // example of how the previous two tests can be combined into one test
        // my preference is to use explicit tests like the ones above unless the method
        // under test is straight forward to test
        setup:
        expect:
            edgeX.mainNode(config) == expectedResult
        where:
            config << [
                [
                    nodes: [
                        []
                    ]
                ], [
                    nodes: [
                        [
                            label: 'soda stereo',
                            isDefault: true
                        ]
                    ]
                ]
            ]
            expectedResult << [
                'centos7-docker-4c-2g',
                'soda stereo'
            ]
    }

    def "Test nodeExists [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.nodeExists(config, 'amd64') == expectedResult
        where:
            config << [
                [
                    nodes: [
                        []
                    ]
                ], [
                    nodes: [
                        [
                            arch: 'arm64'
                        ], [
                            arch: 'arm64'
                        ]
                    ]
                ], [
                    nodes: [
                        [
                            arch: 'arm64'
                        ], [
                            arch: 'amd64'
                        ]
                    ]
                ]
            ]
            expectedResult << [
                false,
                false,
                true
            ]
    }

    def "Test getNode [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.getNode(config, 'amd64') == expectedResult
        where:
            config << [
                [
                    nodes: [
                        []
                    ]
                ], [
                    nodes: [
                        [
                            arch: 'arm64',
                            label: 'mana'
                        ], [
                            arch: 'amd64',
                            label: 'soda stereo'
                        ]
                    ]
                ]
            ]
            expectedResult << [
                'centos7-docker-4c-2g',
                'soda stereo'
            ]
    }

    def "Test setupNodes [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.setupNodes(config) == expectedResult
        where:
            config << [
                [:],
                // request configuration for specific architecture
                [
                    arch: ['amd64']
                ]
            ]
            expectedResult << [
                [
                    [
                        label: 'centos7-docker-4c-2g',
                        arch: 'amd64',
                        isDefault: true
                    ], [
                        label: 'ubuntu18.04-docker-arm64-4c-16g',
                        arch: 'arm64',
                        isDefault: false
                    ]
                ], [
                    [
                        label: 'centos7-docker-4c-2g',
                        arch: 'amd64',
                        isDefault: true
                    ]
                ]
            ]
    }

    def "Test getVmArch [Should] return arm64 [When] running on arm architecture" () {
        setup:
            getPipelineMock('sh')([
                returnStdout: true,
                script: 'uname -m']) >> {
                    'aarch64'
            }
        expect:
            edgeX.getVmArch() == expectedResult
        where:
            expectedResult << [
                'arm64'
            ]
    }

    def "Test getVmArch [Should] return sh result [When] running on non-arm architecture" () {
        setup:
            getPipelineMock('sh')([
                returnStdout: true,
                script: 'uname -m']) >> {
                    'amd64'
            }
        expect:
            edgeX.getVmArch() == expectedResult
        where:
            expectedResult << [
                'amd64'
            ]
    }

    def "Test bannerMessage [Should] call expected [When] called" () {
        setup:
            explicitlyMockPipelineVariable('echo')
        when:
            edgeX.bannerMessage('hello world')
        then:
            1 * getPipelineMock('echo.call').call(
                '=========================================================\n hello world\n=========================================================')
    }

    def "Test printMap [Should] call expected [When] called" () {
        setup:
            explicitlyMockPipelineVariable('echo')
        when:
            def map = [
                name: 'the beatles',
                color: 'blue',
                isDefault: false,
                count: 101
            ]
            edgeX.printMap(map)
        then:
            1 * getPipelineMock('echo.call').call('     name: the beatles\n    color: blue\nisDefault: false\n    count: 101')
    }

    def "Test defaultTrue [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.defaultTrue(value) == expectedResult
        where:
            value << [
                true,
                null,
                false
            ]
            expectedResult << [
                true,
                true,
                false
            ]
    }

    def "Test defaultFalse [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.defaultFalse(value) == expectedResult
        where:
            value << [
                true,
                null,
                false
            ]
            expectedResult << [
                true,
                false,
                false
            ]
    }

    // still working on a better test. right now just confirming the proper script is executed
    def "Test releaseInfo [Should] run releaseinfo.sh shell script with [When] called" () {
        setup:
            explicitlyMockPipelineVariable('usernamePassword')
            explicitlyMockPipelineVariable('withEnv')
            explicitlyMockPipelineVariable('bannerMessage')
            explicitlyMockPipelineVariable('echo')

        when:
            edgeX.releaseInfo()
        then:
            1 * getPipelineMock('sh').call(script: libraryResource('releaseinfo.sh'))
    }
}
