import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXEmailUtilSpec extends JenkinsPipelineSpecification {

    def edgeXEmailUtil = null

    def setup() {
        edgeXEmailUtil = loadPipelineScriptForTest('vars/edgeXEmailUtil.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test getJobDetailsJson [Should] generate expected object with branch override [When] build fails" () {
        setup:
            def environmentVariables = [
                'GIT_BASE':     'https://github.com/edgexfoundry/$PROJECT',
                'GIT_URL':      'git@github.com:edgexfoundry/edgex-global-pipelines.git',
                'GIT_BRANCH':   'override-branch-name',
                'JOB_NAME':     'edgexfoundry/mock-job/master',
                'BUILD_NUMBER': '1',
                'BUILD_URL':    'https://jenkins.edgexfoundry.org/mock-job/1/',
                'JENKINS_URL':  'https://jenkins.edgexfoundry.org'
            ]
            edgeXEmailUtil.getBinding().setVariable('env', environmentVariables)
            edgeXEmailUtil.getBinding().setVariable('currentBuild', [ result: 'FAILURE', durationString: '4m 20s and counting' ])

            setupMockGitCalls()
            setupMockBuildLog()

        expect:
            edgeXEmailUtil.getJobDetailsJson() == expectedResult
        where:
            expectedResult = [
                jobName: "edgexfoundry/mock-job/master #1",
                buildNumber: '1',
                buildUrl: 'https://jenkins.edgexfoundry.org/mock-job/1/',
                gitUrl: 'https://github.com/edgexfoundry/edgex-global-pipelines.git',
                buildConsoleUrl: "https://jenkins.edgexfoundry.org/mock-job/1/console",
                color: '#e60d49',
                status: 'FAILURE',
                author: 'mock-author',
                branch: 'override-branch-name',
                duration: '4m 20s',
                commitMessage: 'mock-message',
                failedStage: 'mock-failed-stage',
                changeLog: ['mock-changelog-item-1', 'mock-changelog-item-2'],
                buildLog: 'mock-build-log'
            ]
    }

    def "Test getJobDetailsJson [Should] generate expected object [When] build fails" () {
        setup:
            def environmentVariables = [
                'GIT_BASE':     'https://github.com/edgexfoundry/$PROJECT',
                'GIT_URL':      'git@github.com:edgexfoundry/edgex-global-pipelines.git',
                'JOB_NAME':     'edgexfoundry/mock-job/master',
                'BUILD_NUMBER': '1',
                'BUILD_URL':    'https://jenkins.edgexfoundry.org/mock-job/1/',
                'JENKINS_URL':  'https://jenkins.edgexfoundry.org'
            ]
            edgeXEmailUtil.getBinding().setVariable('env', environmentVariables)
            edgeXEmailUtil.getBinding().setVariable('currentBuild', [ result: 'FAILURE', durationString: '4m 20s and counting' ])

            setupMockGitCalls()
            setupMockBuildLog()

        expect:
            edgeXEmailUtil.getJobDetailsJson() == expectedResult
        where:
            expectedResult = [
                jobName: "edgexfoundry/mock-job/master #1",
                buildNumber: '1',
                buildUrl: 'https://jenkins.edgexfoundry.org/mock-job/1/',
                gitUrl: 'https://github.com/edgexfoundry/edgex-global-pipelines.git',
                buildConsoleUrl: "https://jenkins.edgexfoundry.org/mock-job/1/console",
                color: '#e60d49',
                status: 'FAILURE',
                author: 'mock-author',
                branch: 'mock-branch',
                duration: '4m 20s',
                commitMessage: 'mock-message',
                failedStage: 'mock-failed-stage',
                changeLog: ['mock-changelog-item-1', 'mock-changelog-item-2'],
                buildLog: 'mock-build-log'
            ]
    }

    def "Test getJobDetailsJson [Should] generate expected object with error handling [When] build fails" () {
        setup:
            def environmentVariables = [
                'GIT_BASE':     'https://github.com/edgexfoundry/$PROJECT',
                'GIT_URL':      'git@github.com:edgexfoundry/edgex-global-pipelines.git',
                'JOB_NAME':     'edgexfoundry/mock-job/master',
                'BUILD_NUMBER': '1',
                'BUILD_URL':    'https://jenkins.edgexfoundry.org/mock-job/1/',
                'JENKINS_URL':  'https://jenkins.edgexfoundry.org'
            ]
            edgeXEmailUtil.getBinding().setVariable('env', environmentVariables)
            edgeXEmailUtil.getBinding().setVariable('currentBuild', [ result: 'FAILURE', durationString: '4m 20s and counting' ])

            setupErrorGitCalls()
            setupErrorBuildLog()

        expect:
            edgeXEmailUtil.getJobDetailsJson() == expectedResult
        where:
            expectedResult = [
                jobName: "edgexfoundry/mock-job/master #1",
                buildNumber: '1',
                buildUrl: 'https://jenkins.edgexfoundry.org/mock-job/1/',
                gitUrl: 'https://github.com/edgexfoundry/edgex-global-pipelines.git',
                buildConsoleUrl: "https://jenkins.edgexfoundry.org/mock-job/1/console",
                color: '#e60d49',
                status: 'FAILURE',
                author: 'Unknown Author',
                branch: 'Unknown Branch',
                duration: '4m 20s',
                commitMessage: 'Unknown Commit Message',
                failedStage: 'Could not determine failed stage',
                changeLog: [],
                buildLog: 'An error occurred getting build log details...'
            ]
    }

    def "Test generateEmailTemplate [Should] generate expected email html template [When] build fails" () {
        setup:
            def environmentVariables = [
                'GIT_BASE':     'https://github.com/edgexfoundry/$PROJECT',
                'GIT_URL':      'git@github.com:edgexfoundry/edgex-global-pipelines.git',
                'JOB_NAME':     'edgexfoundry/mock-job/master',
                'BUILD_NUMBER': '1',
                'BUILD_URL':    'https://jenkins.edgexfoundry.org/mock-job/1/',
                'JENKINS_URL':  'https://jenkins.edgexfoundry.org',
                'WORKSPACE': '/mock/workspace'
            ]
            edgeXEmailUtil.getBinding().setVariable('env', environmentVariables)
            edgeXEmailUtil.getBinding().setVariable('currentBuild', [ result: 'FAILURE', durationString: '4m 20s and counting' ])

            setupMockGitCalls()
            setupMockBuildLog()

            getPipelineMock('libraryResource')('email/build-notification-template.html') >> {
                return 'mock-template'
            }

            getPipelineMock('docker.image')('node:alpine') >> explicitlyMockPipelineVariable('DockerImageMock')

            def jobJsonMock = [
                jobName: "edgexfoundry/mock-job/master #1",
                buildNumber: '1',
                buildUrl: 'https://jenkins.edgexfoundry.org/mock-job/1/',
                gitUrl: 'https://github.com/edgexfoundry/edgex-global-pipelines.git',
                buildConsoleUrl: "https://jenkins.edgexfoundry.org/mock-job/1/console",
                color: '#e60d49',
                status: 'FAILURE',
                author: 'mock-author',
                branch: 'mock-branch',
                duration: '4m 20s',
                commitMessage: 'mock-message',
                failedStage: 'mock-failed-stage',
                changeLog: ['mock-changelog-item-1', 'mock-changelog-item-2'],
                buildLog: 'mock-build-log'
            ]

        when:
            

            edgeXEmailUtil.generateEmailTemplate()
        then:
            1 * getPipelineMock('writeJSON').call(file: '/tmp/jobdetails-1.json', json: jobJsonMock)
            1 * getPipelineMock('writeFile').call(file: '/tmp/job-email-template.html', text: 'mock-template')
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "-u 0:0 -v /tmp:/tmp" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('npm install -g mustache')
            1 * getPipelineMock('sh').call('mustache /tmp/jobdetails-1.json /tmp/job-email-template.html /mock/workspace/email-rendered.html')
            1 * getPipelineMock('readFile').call('./email-rendered.html')
    }

    def setupMockGitCalls() {
        getPipelineMock('sh').call([script: 'git rev-parse HEAD', returnStdout:true]) >> 'mock-git-commit'
        getPipelineMock('sh').call([script: 'git --no-pager show -s --format=\'%an\' mock-git-commit', returnStdout:true]) >> 'mock-author'
        getPipelineMock('sh').call([script: 'git log -1 --pretty=%B', returnStdout:true]) >> 'mock-message'
        getPipelineMock('sh').call([script: 'git log -m -1 --name-only --pretty=\'format:\' mock-git-commit', returnStdout:true]) >> 'mock-changelog-item-1\nmock-changelog-item-2'
        getPipelineMock('sh').call([script: 'git rev-parse --abbrev-ref HEAD', returnStdout:true]) >> 'mock-branch'
    }

    def setupErrorGitCalls() {
        getPipelineMock('sh').call([script: 'git rev-parse HEAD', returnStdout:true]) >> 'mock-git-commit'

        getPipelineMock('sh').call([script: 'git --no-pager show -s --format=\'%an\' mock-git-commit', returnStdout:true]) >> {
            throw new Exception('error')
        }

        getPipelineMock('sh').call([script: 'git log -1 --pretty=%B', returnStdout:true]) >> {
            throw new Exception('error')
        }

        getPipelineMock('sh').call([script: 'git log -m -1 --name-only --pretty=\'format:\' mock-git-commit', returnStdout:true]) >> {
            throw new Exception('error')
        }

        getPipelineMock('sh').call([script: 'git rev-parse --abbrev-ref HEAD', returnStdout:true]) >> {
            throw new Exception('error')
        }
    }

    def setupMockBuildLog() {
        getPipelineMock('libraryResource')('build-log-extract.sh') >> {
            return 'build-log-extract'
        }

        // mock the format of build-log-extract.sh
        getPipelineMock('sh').call([script: 'build-log-extract', returnStdout:true]) >> '''mock-failed-stage
^^^^^^^^^^^^
mock-build-log'''
    }

    def setupErrorBuildLog() {
        getPipelineMock('libraryResource')('build-log-extract.sh') >> {
            return 'build-log-extract'
        }

        // mock the format of build-log-extract.sh
        getPipelineMock('sh').call([script: 'build-log-extract', returnStdout:true]) >> ''
    }
}
