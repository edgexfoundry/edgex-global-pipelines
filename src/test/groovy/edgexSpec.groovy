import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSpec extends JenkinsPipelineSpecification {

    def edgeX = null

    def setup() {
        edgeX = loadPipelineScriptForTest('vars/edgex.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test isReleaseStream [Should] return expected [When] called in production" () {
        setup:
            def environmentVariables = [
                'SILO': 'production'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isReleaseStream('master') == true
            edgeX.isReleaseStream('main') == true
            edgeX.isReleaseStream('california') == true
            edgeX.isReleaseStream('delhi') == true
            edgeX.isReleaseStream('edinburgh') == true
            edgeX.isReleaseStream('fuji') == true
            edgeX.isReleaseStream('geneva') == true
            edgeX.isReleaseStream('hanoi') == true
            edgeX.isReleaseStream('xyzmaster') == false
            edgeX.isReleaseStream('masterxyz') == false
            edgeX.isReleaseStream('xyzmasterxyz') == false
    }

    def "Test isReleaseStream [Should] return expected [When] called in non-production" () {
        setup:
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isReleaseStream('master') == false
            edgeX.isReleaseStream('main') == false
            edgeX.isReleaseStream('california') == false
            edgeX.isReleaseStream('delhi') == false
            edgeX.isReleaseStream('edinburgh') == false
            edgeX.isReleaseStream('fuji') == false
            edgeX.isReleaseStream('geneva') == false
            edgeX.isReleaseStream('hanoi') == false
            edgeX.isReleaseStream('xyzmaster') == false
            edgeX.isReleaseStream('masterxyz') == false
            edgeX.isReleaseStream('xyzmasterxyz') == false
    }

    def "Test isReleaseStream [Should] return expected [When] called without branchName in production" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'us5375'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

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
                script: 'git diff --name-only 6c48b4195c2eda681d9817e490d6fbb8042956fc origin/master | grep \"test\" | wc -l'
            ]) >> {
                '0'
            }

        expect:
            edgeX.didChange('test') == expectedResult

        where:
            expectedResult << [
                false
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
        when:
            edgeX.bannerMessage('hello world')
        then:
            1 * getPipelineMock('echo').call(
                '=========================================================\n hello world\n=========================================================')
    }

    def "Test printMap [Should] call expected [When] called" () {
        setup:
        when:
            def map = [
                name: 'the beatles',
                color: 'blue',
                isDefault: false,
                count: 101
            ]
            edgeX.printMap(map)
        then:
            1 * getPipelineMock('echo').call('     name: the beatles\n    color: blue\nisDefault: false\n    count: 101')
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
            explicitlyMockPipelineVariable('bannerMessage')
        when:
            edgeX.releaseInfo()
        then:
            1 * getPipelineMock('sh').call(script: libraryResource('releaseinfo.sh'))
    }

    def "Test isDryRun [Should] return false [When] DRY_RUN has false values" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': ''
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
            def values = [
                '0',
                'false',
                'other'
            ]
        expect:
            values.each { value ->
                edgeX.getBinding().setVariable(env.DRY_RUN, value)
                edgeX.isDryRun() == false
            }
    }

    def "Test isDryRun [Should] return true [When] DRY_RUN has true values" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': ''
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
            def values = [
                null,
                '1',
                'true'
            ]
        expect:
            values.each { value ->
                edgeX.getBinding().setVariable(env.DRY_RUN, value)
                edgeX.isDryRun() == true
            }
    }

    def "Test isMergeCommit [Should] return true [When] commit is a merge commit" () {
        setup:
            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git rev-list -1 --merges MergeCommitSha~1..MergeCommitSha'
            ]) >> {
                'MergeCommitSha'
            }

        expect:
            edgeX.isMergeCommit('MergeCommitSha') == expectedResult

        where:
            expectedResult << [
                true
            ]
    }

    def "Test isMergeCommit [Should] return false [When] commit is not a merge commit" () {
        setup:
            def environmentVariables = [
                GIT_COMMIT: 'RegularCommitSha'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('sh')([
                    returnStdout: true,
                    script: 'git rev-list -1 --merges RegularCommitSha~1..RegularCommitSha'
            ]) >> {
                'AnotherCommit'
            }

        expect:
            edgeX.isMergeCommit('RegularCommitSha') == expectedResult

        where:
            expectedResult << [
                false
            ]
    }

    def "Test getPreviousCommit [Should] the previous commit [When] doing a merge commit" () {
        setup:
            explicitlyMockPipelineVariable('isMergeCommit')

            // mock isMergeCommit
            getPipelineMock('sh')([
                    returnStdout: true,
                    script: 'git rev-list -1 --merges MergeCommitSha~1..MergeCommitSha'
            ]) >> {
                'MergeCommitSha'
            }

            getPipelineMock('sh')([
                    returnStdout: true,
                    script: "git rev-list --parents -n 1 MergeCommitSha | cut -d' ' -f3"
            ]) >> {
                'SomePreviousCommitSha'
            }

        expect:
            edgeX.getPreviousCommit('MergeCommitSha') == expectedResult
        where:
            expectedResult = 'SomePreviousCommitSha'
    }

    def "Test getPreviousCommit [Should] the previous commit [When] doing a regular/squash commit" () {
        setup:
            explicitlyMockPipelineVariable('isMergeCommit')

            getPipelineMock('sh')([
                    returnStdout: true,
                    script: 'git show --pretty=%H HEAD~1 | xargs'
            ]) >> {
                'SomePreviousCommit'
            }

        expect:
            edgeX.getPreviousCommit('RegularCommitSha') == expectedResult
        where:
            expectedResult = 'SomePreviousCommit'
    }

    def "Test getBranchName [Should] return the current branch name" () {
        setup:
            explicitlyMockPipelineVariable('getCommitMessage')

            getPipelineMock('sh')([
                    returnStdout: true,
                    script: 'git rev-parse --abbrev-ref HEAD'
            ]) >> {
                'MyBranch'
            }

        expect:
            edgeX.getBranchName() == expectedResult
        where:
            expectedResult = 'MyBranch'
    }

    def "Test getCommitMessage [Should] return the commit message [When] the commit sha is passed in" () {
        setup:
            explicitlyMockPipelineVariable('getCommitMessage')

            getPipelineMock('sh')([
                    returnStdout: true,
                    script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'MyCommitMessage'
            }

        expect:
            edgeX.getCommitMessage('MyCommitSha') == expectedResult
        where:
            expectedResult = 'MyCommitMessage'
    }

    // isBuildCommit should return true for strings that match the "build(...): ..." pattern only
    def "Test isBuildCommit [Should] return expected [When] called" () {
        setup:
        expect:
            edgeX.isBuildCommit(value) == expectedResult
        where:
            value << [
                'release(geneva): Release Device Grove C service (1.2.0) and Testing frameworks (1.2.1)',
                'Merge pull request #46 from lranjbar/geneva-release',
                'release(geneva dot): Release App Service Configurable to latest track',
                'build(geneva): [1.0.0,stable] Stage Artifacts for App Service Configurable',
                'build(hanoi): [1.2.0,dev] Stage Artifacts for edgex-go',
                'Merge pull request #46 from lranjbar/build(hanoi): release'
            ]
            expectedResult << [
                false,
                false,
                false,
                true,
                true,
                false
            ]
    }

    def "Test parseBuildCommit [Should] return expected [When] called" () {
        setup:
        expect:
                edgeX.parseBuildCommit(value) == expectedResult
        where:
            value << [
                'build(geneva): [1.0.0,stable] Stage Artifacts for App Service Configurable',
                'build(hanoi): [1.2.0,rc] Stage Artifacts for edgex-go',
                'build(hanoi): [1.2.0-dev.1,dev] Stage Artifacts for edgex-go'
            ]
            expectedResult << [
                [version:'1.0.0', namedTag:'stable'],
                [version:'1.2.0', namedTag:'rc'],
                [version:'1.2.0-dev.1', namedTag:'dev']
            ]
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in release(geneva) commit" () {
        setup:
        when:
            edgeX.parseBuildCommit('release(geneva): Release Device Grove C service (1.2.0) and Testing frameworks (1.2.1)')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in release(geneva) PR commit" () {
        setup:
        when:
            edgeX.parseBuildCommit('Merge pull request #46 from lranjbar/geneva-release')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in release(geneva dot) commit" () {
        setup:
        when:
            edgeX.parseBuildCommit('release(geneva dot): Release App Service Configurable to latest track')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in build(hanoi): PR commit" () {
        setup:
        when:
            edgeX.parseBuildCommit('Merge pull request #46 from lranjbar/build(hanoi): release')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in build(hanoi) commit" () {
        setup:
        when:
            edgeX.parseBuildCommit('build(hanoi): [1.2.0-dev.1,1.0.0] Stage Artifacts for edgex-go')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test getTmpDir [Should] return a directory name [When] called" () {
        setup:
        getPipelineMock('sh')([
                returnStdout: true,
                script: 'mktemp -d -t ci-XXXXX']) >>
        {
            '/tmp/ci-20jBb'
        }

        expect:
            edgeX.getTmpDir() =~ expectedResult

        where:
            expectedResult << [
                '/tmp/ci-'
            ]
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and true alpine flag" () {
        setup:
        expect:
            edgeX.getGoLangBaseImage(version, true) == expectedResult
        where:
            version << [
                '1.11',
                '1.12',
                '1.13',
                '1.15',
                '1.01',
                'MyVersion'
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.11.13-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.15-alpine',
                'golang:1.01-alpine',
                'golang:MyVersion-alpine'
            ]
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and false alpine flag" () {
        setup:
        expect:
            edgeX.getGoLangBaseImage(version, false) == expectedResult
        where:
            version << [
                '1.11',
                '1.12',
                '1.13',
                '1.15',
                '1.01',
                'MyVersion'
            ]
            expectedResult << [
                'golang:1.11',
                'golang:1.12',
                'golang:1.13',
                'golang:1.15',
                'golang:1.01',
                'golang:MyVersion'
            ]
    }

}
