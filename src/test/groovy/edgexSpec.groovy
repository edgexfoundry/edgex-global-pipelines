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
            edgeX.isReleaseStream('xyzmain') == false
            edgeX.isReleaseStream('mainxyz') == false
            edgeX.isReleaseStream('xyzmainxyz') == false
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
            edgeX.isReleaseStream('xyzmain') == false
            edgeX.isReleaseStream('mainxyz') == false
            edgeX.isReleaseStream('xyzmainxyz') == false
    }

    def "Test isReleaseStream [Should] return expected [When] called without branchName in production" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'us5375'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isReleaseStream() == false
    }

    def "Test isLTS [Should] return expected [When] called" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS() == false
    }

    def "Test isLTS [Should] return expected [When] called for PRs" () {
        setup:
            def environmentVariables = [
                'CHANGE_ID': '1124',
                'CHANGE_TARGET': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS() == false
    }

    def "Test isLTS [Should] return expected [When] called" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'jakarta'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS() == true
    }

    def "Test isLTS [Should] return expected [When] called for PRs" () {
        setup:
            def environmentVariables = [
                'CHANGE_ID': '1124',
                'CHANGE_TARGET': 'jakarta'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS() == true
    }

    def "Test isLTS [Should] return expected [When] called with lts branchOverride" () {
        setup:
            def environmentVariables = [
                'CHANGE_ID': '1124',
                'CHANGE_TARGET': 'mock',
                'GIT_BRANCH': 'mock'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS('jakarta') == true
    }

    def "Test isLTS [Should] return expected [When] called with non-lts branchOverride" () {
        setup:
            def environmentVariables = [
                'CHANGE_ID': '1124',
                'CHANGE_TARGET': 'jakarta',
                'GIT_BRANCH': 'jakarta'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.isLTS('mock') == false
    }

    def "Test getTargetBranch [Should] return expected [When] called for branch builds" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getTargetBranch() == 'main'
    }

    def "Test getTargetBranch [Should] return expected [When] called for PR builds" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main',
                'CHANGE_ID': '1124',
                'CHANGE_TARGET': 'PR-1124'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getTargetBranch() == 'PR-1124'
    }

    def "Test didChange [Should] return true [When] there is no previous commit" () {
        expect:
            edgeX.didChange('test', null) == expectedResult

        where:
            expectedResult << [
                true
            ]
    }

    def "Test didChange [Should] return false [When] previous commit on non main branch has no changes" () {
        setup:
            def environmentVariables = [
                GIT_BRANCH: 'us5375',
                GIT_COMMIT: '6c48b4195c2eda681d9817e490d6fbb8042956fc'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git diff --name-only 6c48b4195c2eda681d9817e490d6fbb8042956fc origin/main | grep \"test\" | wc -l'
            ]) >> {
                '0'
            }

        expect:
            edgeX.didChange('test') == false
    }

    def "Test mainNode [Should] return default label [When] empty nodes are passed in" () {
        expect:
            edgeX.mainNode([nodes: [[]]]) == expectedResult
        where:
            expectedResult << [
                'ubuntu20.04-docker-8c-8g'
            ]
    }

    def "Test mainNode [Should] return node label [When] list that contains a default node is passed in" () {
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
                'ubuntu20.04-docker-8c-8g',
                'soda stereo'
            ]
    }

    def "Test nodeExists [Should] return expected [When] called" () {
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
                'ubuntu20.04-docker-8c-8g',
                'soda stereo'
            ]
    }

    def "Test setupNodes [Should] return expected [When] called" () {
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
                        label: 'ubuntu20.04-docker-8c-8g',
                        arch: 'amd64',
                        isDefault: true
                    ], [
                        label: 'ubuntu20.04-docker-arm64-4c-16g',
                        arch: 'arm64',
                        isDefault: false
                    ]
                ], [
                    [
                        label: 'ubuntu20.04-docker-8c-8g',
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
        when:
            edgeX.bannerMessage('hello world')
        then:
            1 * getPipelineMock('echo').call(
                '=========================================================\n hello world\n=========================================================')
    }

    def "Test printMap [Should] call expected [When] called" () {
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
                    script: 'git show --pretty=%H HEAD~1 | head -n 1 | xargs'
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
        when:
            edgeX.parseBuildCommit('release(geneva): Release Device Grove C service (1.2.0) and Testing frameworks (1.2.1)')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in release(geneva) PR commit" () {
        when:
            edgeX.parseBuildCommit('Merge pull request #46 from lranjbar/geneva-release')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in release(geneva dot) commit" () {
        when:
            edgeX.parseBuildCommit('release(geneva dot): Release App Service Configurable to latest track')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in build(hanoi): PR commit" () {
        when:
            edgeX.parseBuildCommit('Merge pull request #46 from lranjbar/build(hanoi): release')
        then:
            1 * getPipelineMock('error').call('[edgex.parseBuildCommit]: No matches found.')
    }

    def "Test parseBuildCommit [Should] raise error [When] no matches are found in build(hanoi) commit" () {
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

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and true alpine flag and on non lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getGoLangBaseImage(version, true) == expectedResult
        where:
            version << [
                '1.11',
                '1.12',
                '1.13',
                '1.15',
                '1.16',
                '1.17',
                '1.18',
                '1.01',
                'MyVersion'
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.11.13-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.12.14-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.13-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.15-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.16-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.17-alpine',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.18-alpine',
                'golang:1.01-alpine',
                'golang:MyVersion-alpine'
            ]
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and false alpine flag and on non lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getGoLangBaseImage(version, false) == expectedResult
        where:
            version << [
                '1.11',
                '1.12',
                '1.13',
                '1.15',
                '1.16',
                '1.17',
                '1.18',
                '1.01',
                'MyVersion'
            ]
            expectedResult << [
                'golang:1.11',
                'golang:1.12',
                'golang:1.13',
                'golang:1.15',
                'golang:1.16',
                'golang:1.17',
                'golang:1.18',
                'golang:1.01',
                'golang:MyVersion'
            ]
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and true alpine flag and on lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'lts-test'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeX.getGoLangBaseImage(version, true) == expectedResult
        where:
            version << [
                '1.15',
                '1.16',
                'MyVersion'
            ]
            expectedResult << [
                'golang:1.15-alpine',
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.16-alpine-lts',
                'golang:MyVersion-alpine'
            ]
    }

    def "Test getGoLangBaseImage [Should] return expected #expectedResult [When] called with with version #version and true alpine flag and branchOverride and on lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'foo'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
        expect:
            edgeX.getGoLangBaseImage(version, true, branchOverride) == expectedResult
        where:
            version << [
                '1.16',
                '1.17',
                'MyVersion'
            ]
            branchOverride << [
                'lts-test',
                'jakarta',
                null
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.16-alpine-lts',
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.17-alpine-lts',
                'golang:MyVersion-alpine'
            ]
    }

    def "Test getGoModVersion [Should] extract Go version from go.mod file [When] called" () {
        setup:
            getPipelineMock('sh')([
                returnStdout: true,
                script: "grep '^go [0-9].[0-9]*' go.mod | cut -d' ' -f 2"]) >> '1.17'
        expect:
            edgeX.getGoModVersion() == '1.17'
    }

    def "Test isGoProject [Should] call with expected arguments [When] go.mod exists with no folder" () {
        setup:
            getPipelineMock('fileExists').call('go.mod') >> true
        expect:
            edgeX.isGoProject() == true

    }

    def "Test isGoProject [Should] call with expected arguments [When] go.mod does not exist with no folder" () {
        setup:
            getPipelineMock('fileExists').call('go.mod') >> false
        expect:
            edgeX.isGoProject() == false

    }

    def "Test getCBaseImage [Should] return expected #expectedResult [When] called with with version #version and on non lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'main'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getCBaseImage(version) == expectedResult
        where:
            version << [
                'latest',
                '1.0',
                'MyVersion'
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:latest',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:1.0',
                'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-gcc-base:MyVersion'
            ]
    }

    def "Test getCBaseImage [Should] return expected #expectedResult [When] called with with version #version and on lts branch" () {
        setup:
            def environmentVariables = [
                'GIT_BRANCH': 'lts-test'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeX.getCBaseImage(version) == expectedResult
        where:
            version << [
                'latest',
                '1.0',
                'MyVersion'
            ]
            expectedResult << [
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-gcc-base:gcc-lts',
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-gcc-base:gcc-lts',
                'nexus3.edgexfoundry.org:10002/edgex-devops/edgex-gcc-base:gcc-lts'
            ]
    }

    def "Test parallelJobCost [Should] call lfParallelCostCapture inside a docker image [When] called with no params" () {
        setup:
            explicitlyMockPipelineVariable('lfParallelCostCapture')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools-log-publisher:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeX.parallelJobCost()
        then:
            1 * getPipelineMock('lfParallelCostCapture.call').call()
    }

    def "Test parallelJobCost [Should] call lfParallelCostCapture inside a docker image [When] called with no tag parameter" () {
        setup:
            explicitlyMockPipelineVariable('lfParallelCostCapture')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools-log-publisher:arm64') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeX.parallelJobCost('arm64')
        then:
            1 * getPipelineMock('lfParallelCostCapture.call').call()
    }

    def "Test isLTSReleaseBuild [Should] call expected [When] the commit message matches lts release message" () {
        setup:
            explicitlyMockPipelineVariable('isMergeCommit')
            explicitlyMockPipelineVariable('getCommitMessage')

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'ci(lts-release): mock lts release'
            }

            getPipelineMock('isMergeCommit').call(_) >> true
        expect:
            edgeX.isLTSReleaseBuild('MyCommitSha') == true
    }

    def "Test isLTSReleaseBuild [Should] call expected [When] the commit message matches lts release message and CommitId env var is set" () {
        setup:
            def environmentVariables = [
                'CommitId': 'OverrideCommitSha'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            explicitlyMockPipelineVariable('isMergeCommit')
            explicitlyMockPipelineVariable('getCommitMessage')

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'ci(lts-release): mock lts release'
            }

            getPipelineMock('isMergeCommit').call(_) >> true
        expect:
            edgeX.isLTSReleaseBuild('MyCommitSha') == false
    }

    def "Test isLTSReleaseBuild [Should] call expected [When] the commit message does not match lts release message" () {
        setup:
            explicitlyMockPipelineVariable('isMergeCommit')
            explicitlyMockPipelineVariable('getCommitMessage')

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'ci: mock regular commit not lts-release'
            }

            getPipelineMock('isMergeCommit').call(_) >> true
        expect:
            edgeX.isLTSReleaseBuild('MyCommitSha') == false
    }

    def "Test semverPrep [Should] call expected [When] the commit message is not a build commit message" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyCommitSha'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            explicitlyMockPipelineVariable('isMergeCommit')
            explicitlyMockPipelineVariable('getCommitMessage')
            explicitlyMockPipelineVariable('isBuildCommit')

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'ci: mock regular commit'
            }

            getPipelineMock('isMergeCommit').call(_) >> true
            getPipelineMock('isBuildCommit').call(_) >> false
        expect:
            edgeX.semverPrep() == null
            environmentVariables.BUILD_STABLE_DOCKER_IMAGE == false
    }

    def "Test semverPrep [Should] call expected [When] the commit message is a build commit message" () {
        setup:
            def environmentVariables = [
                'GIT_COMMIT': 'MyCommitSha'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)

            explicitlyMockPipelineVariable('isMergeCommit')
            explicitlyMockPipelineVariable('getCommitMessage')
            explicitlyMockPipelineVariable('isBuildCommit')
            explicitlyMockPipelineVariable('parseBuildCommit')

            getPipelineMock('sh')([
                returnStdout: true,
                script: 'git log --format=format:%s -1 MyCommitSha'
            ]) >> {
                'build(mock): [1.0.0,stable] Stage Artifacts for Mock'
            }

            getPipelineMock('isMergeCommit').call(_) >> true
            getPipelineMock('isBuildCommit').call(_) >> false
            getPipelineMock('parseBuildCommit').call(_) >> [version:'1.0.0', namedTag:'stable']
        expect:
            edgeX.semverPrep() == '1.0.0'
            environmentVariables.NAMED_TAG == 'stable'
            environmentVariables.BUILD_STABLE_DOCKER_IMAGE == true
    }

    def "Test waitFor [Should] call shell script with expected arguments [When] command is provided" () {
        when:
            edgeX.waitFor('mock-command')
        then:
            1 * getPipelineMock('libraryResource').call('wait-for.sh') >> 'wait-for-script'
            1 * getPipelineMock('writeFile').call(['file': './wait-for.sh', 'text': 'wait-for-script'])
            1 * getPipelineMock('sh').call('chmod +x ./wait-for.sh')
            1 * getPipelineMock('sh').call('./wait-for.sh \'mock-command\' 0 5')
    }

    def "Test waitFor [Should] call shell script with expected arguments [When] command, timeout, exit code and sleepFor are provided" () {
        when:
            edgeX.waitFor('mock-command', 60, 99, 20)
        then:
            1 * getPipelineMock('libraryResource').call('wait-for.sh') >> 'wait-for-script'
            1 * getPipelineMock('writeFile').call(['file': './wait-for.sh', 'text': 'wait-for-script'])
            1 * getPipelineMock('sh').call('chmod +x ./wait-for.sh')
            1 * getPipelineMock('sh').call('./wait-for.sh \'mock-command\' 99 20')
    }

    def "Test waitForImages [Should] call shell script with expected arguments [When] images are provided" () {
        when:
            edgeX.waitForImages(['mock-image-1', 'mock-image-2'])
        then:
            1 * getPipelineMock('libraryResource').call('wait-for-images.sh') >> 'wait-for-script'
            1 * getPipelineMock('writeFile').call(['file': './wait-for-images.sh', 'text': 'wait-for-script'])
            1 * getPipelineMock('sh').call('chmod +x ./wait-for-images.sh')
            1 * getPipelineMock('sh').call('./wait-for-images.sh mock-image-1 mock-image-2')
    }

    def "Test createPR [Should] push branch and create pull request [When] called " () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'false'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeX.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2', 'label1,label2')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials': ['edgex-jenkins-ssh']] == _arguments[0][0]
            }
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "--entrypoint=" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git push origin mock-branch')
            1 * getPipelineMock('sh').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'label1,label2'")
    }

    def "Test createPR [Should] push branch and create pull request [When] called with custom credentials" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'false'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeX.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2', 'label1,label2','customPushCredentials')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials': ['customPushCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "--entrypoint=" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git push origin mock-branch')
            1 * getPipelineMock('sh').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'label1,label2'")
    }

    def "Test createPR [Should] mock push branch and create pull request [When] called with DRY_RUN" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'true'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
        when:
            edgeX.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2', 'label1,label2')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('echo').call('git push origin mock-branch')
            1 * getPipelineMock('echo').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'label1,label2'")
    }

    def "Test commitChange [Should] commit changes [When] called" () {
        when:
            def environmentVariables = [
                'DRY_RUN': 'false'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
            edgeX.commitChange('ci: mock commit message')
        then:
            1 * getPipelineMock('sh').call('''
                git add .
                if ! git diff-index --quiet HEAD --; then
                    git commit -s -m 'ci: mock commit message'
                else
                    echo 'No changes detected to commit'
                    exit 1
                fi
            '''.stripIndent())
    }

    def "Test commitChange [Should] commit changes [When] called with DRY_RUN" () {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'true'
            ]
            edgeX.getBinding().setVariable('env', environmentVariables)
        when:
            edgeX.commitChange('ci: mock commit message')
        then:
            1 * getPipelineMock('echo').call("git commit -s -m 'ci: mock commit message'")
    }
}
