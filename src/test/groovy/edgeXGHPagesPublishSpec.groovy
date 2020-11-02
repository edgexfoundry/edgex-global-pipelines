import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

public class EdgeXGHPagesPublishSpec extends JenkinsPipelineSpecification {

    def edgeXGHPagesPublish = null
    def repoUrl = 'git@github.com:edgexfoundry/testrepo.git'
    def gitMock = null
    def credentialId = 'test-user'

    def setup() {
        edgeXGHPagesPublish = loadPipelineScriptForTest('vars/edgeXGHPagesPublish.groovy')
        gitMock = explicitlyMockPipelineVariable('git')
    }

    def "Test edgeXGHPagesPublish [Should] call all steps [When] called with expected arguments"() {
        setup:
            def environmentVariables = [
                'DRY_RUN': 'true'
            ]
            getPipelineMock('sh')([script: 'git log --format=%B -n 1 | grep -v Signed-off-by | ' +
                'head -n 1', returnStdout: true]) >> 'Merge PR'
            edgeXGHPagesPublish.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXGHPagesPublish(repoUrl: "${repoUrl}", credentialId: "${credentialId}")
        then:
            1 * getPipelineMock('dir').call(_) >> { _arguments ->
                assert 'gh-pages-src' == _arguments[0][0]
            }
            1 * getPipelineMock("unstash").call('site-contents') >> true
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'DRY_RUN=true',
                    'GH_PAGES_BRANCH=gh-pages',
                    'COMMIT_MSG=Merge PR'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials': ['test-user']] == _arguments[0][0]
            }
            1 * getPipelineMock("libraryResource").call('github-pages-publish.sh') >> 'edgex-gh-pages-script'
            1 * getPipelineMock('sh').call(script: 'edgex-gh-pages-script')
            1 * getPipelineMock("git.call").call(
                [url : "${repoUrl}", branch: 'gh-pages', credentialsId: "${credentialId}", changelog: false,
                 poll: false])
    }

    def "Test edgeXGHPagesPublish [Should] return error [When] failed with an exception"() {
        setup:
            def exe = new Exception("Error")
            getPipelineMock("libraryResource").call('github-pages-publish.sh') >> 'edgex-gh-pages-script'
            getPipelineMock('sh').call(script: 'edgex-gh-pages-script')
            getPipelineMock('sshagent').call(_) >> { throw exe }
            getPipelineMock("git.call").call(
                [url : "${repoUrl}", branch: 'gh-pages', credentialsId: "${credentialId}", changelog: false,
                 poll: false])
        when:
            edgeXGHPagesPublish(repoUrl: "${repoUrl}")
        then:
            1 * getPipelineMock('error').call('[edgeXGHPagesPublish]: ERROR occurred when publishing ' +
                'to GH Pages: ' + "${exe}")
    }

    def "Test edgeXGHPagesPublish [Should] raise error [When] repoUrl is missing in config"() {
        setup:
        when:
            edgeXGHPagesPublish()
        then:
            1 * getPipelineMock('error').call('[edgeXGHPagesPublish]: Repository URL missing in config')
    }
}
