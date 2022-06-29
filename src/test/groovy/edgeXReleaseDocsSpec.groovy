import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseDocsSpec extends JenkinsPipelineSpecification {
    def edgeXReleaseDocs
    def validReleaseYaml

    def setup() {
        edgeXReleaseDocs = loadPipelineScriptForTest('vars/edgeXReleaseDocs.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXDocker')

        validReleaseYaml = [
            name:'edgex-docs',
            version:'2.2.0',
            releaseName:'currentRelease',
            releaseStream:'main',
            commitId: 'c0818f6da75fef2ffe509345f5fc87075bcd5114',
            repo:'https://github.com/edgexfoundry/edgex-docs.git',
            gitTag: false,
            docs:true,
            docsInfo: [
                nextReleaseVersion:'2.3.0',
                nextReleaseName:'nextRelease',
                reviewers:'mock-reviewer'
            ]
        ]
    }

    def "Test createPR [Should] push branch and create pull request [When] called " () {
        setup:
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseDocs.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials': ['edgex-jenkins-ssh']] == _arguments[0][0]
            }
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "--entrypoint=" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git push origin mock-branch')
            1 * getPipelineMock('sh').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'ci,documentation'")
    }

    def "Test createPR [Should] push branch and create pull request [When] called with custom credentials" () {
        setup:
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseDocs.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2', 'customPushCredentials')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials': ['customPushCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                assert "--entrypoint=" == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git push origin mock-branch')
            1 * getPipelineMock('sh').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'ci,documentation'")
    }

    def "Test createPR [Should] mock push branch and create pull request [When] called with DRY_RUN" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseDocs.createPR('mock-branch', 'mock title', 'mock message', 'reviwer1,reviwer2')
        then:
            // Going to skip testing commit change in this function because it is already covered in another test
            1 * getPipelineMock('echo').call('git push origin mock-branch')
            1 * getPipelineMock('echo').call("gh pr create --base main --head mock-branch --title 'mock title' --body 'mock message' --reviewer 'reviwer1,reviwer2' --label 'ci,documentation'")
    }

    def "Test commitChange [Should] commit changes [When] called" () {
        setup:
        when:
            edgeXReleaseDocs.commitChange('ci: mock commit message')
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
            getPipelineMock('edgex.isDryRun').call() >> true
        when:
            edgeXReleaseDocs.commitChange('ci: mock commit message')
        then:
            1 * getPipelineMock('echo').call("git commit -s -m 'ci: mock commit message'")
    }

    def "Test publishReleaseBranch [Should] execute expected [When] called" () {
        setup:
        when:
            edgeXReleaseDocs.publishReleaseBranch(validReleaseYaml)
        then:
            1 * getPipelineMock('sh').call('git checkout c0818f6da75fef2ffe509345f5fc87075bcd5114')
            1 * getPipelineMock('sh').call('git checkout -b currentRelease')
            1 * getPipelineMock('sh').call('rm -rf docs')
            1 * getPipelineMock('sh').call('git status')
            1 * getPipelineMock('sh').call('git push origin currentRelease')
    }

    def "Test publishVersionChangesPR [Should] execute expected [When] called" () {
        setup:
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock('sh')([script: "jq '. += [{\"version\": \"2.3\", \"title\": \"2.3-NextRelease\", \"aliases\": []}]' docs/versions.json", returnStdout: true]) >> '''
            [
                {
                    "version": "2.0",
                    "title": "2.0-Ireland",
                    "aliases": []
                },
                {
                    "version": "2.1",
                    "title": "2.1-Jakarta",
                    "aliases": []
                },
                {
                    "version": "2.2",
                    "title": "2.2-Kamakura",
                    "aliases": []
                },
                {
                    "version": "2.3",
                    "title": "2.3-NextRelease",
                    "aliases": []
                }
            ]
            '''.stripIndent()
        when:
            edgeXReleaseDocs.publishVersionChangesPR(validReleaseYaml)
        then:
            1 * getPipelineMock('sh').call('git reset --hard c0818f6da75fef2ffe509345f5fc87075bcd5114')
            1 * getPipelineMock('sh').call('git checkout -b currentRelease-version-changes')
            1 * getPipelineMock('sh').call("sed -E -i 's|replace\\(\".*\"\\)|replace\\(\"2.2\"\\)|g' docs/index.html")
            1 * getPipelineMock('sh').call("sed -i 's|2.2|2.3|g' mkdocs.yml")

            1 * getPipelineMock('writeFile').call(['file': 'docs/versions.json',  'text':'\n[ {     "version": "2.0",     "title": "2.0-Ireland",     "aliases": [] }, {     "version": "2.1",     "title": "2.1-Jakarta",     "aliases": [] }, {     "version": "2.2",     "title": "2.2-Kamakura",     "aliases": [] }, {     "version": "2.3",     "title": "2.3-NextRelease",     "aliases": [] }\n]\n'])

            1 * getPipelineMock('sh').call('git diff')
    }

    def "Test publishSwaggerChangesPR [Should] execute expected [When] called" () {
        setup:
            getPipelineMock('docker.image')('ghcr.io/supportpal/github-gh-cli') >> explicitlyMockPipelineVariable('DockerImageMock')
        when:
            edgeXReleaseDocs.publishSwaggerChangesPR(validReleaseYaml)
        then:
            1 * getPipelineMock('sh').call('git reset --hard c0818f6da75fef2ffe509345f5fc87075bcd5114')
            1 * getPipelineMock('sh').call('git checkout -b currentRelease-swagger-changes')
            1 * getPipelineMock('sh').call("""
            for file in \$(find docs_src/api -name '*.md'); do
                echo "Processing \${file}"
                sed -E -i 's|EdgeXFoundry1/(.*)/2.2.0|EdgeXFoundry1/\\1/2.3.0|g' \${file}
            done
            """.stripIndent())

            1 * getPipelineMock('sh').call('git diff')
    }

    def "Test validate [Should] raise error [When] release info yaml contains gitTag attribute" () {
        setup:
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.gitTag = true
            edgeXReleaseDocs.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDocs] Cannot publish docs and use gitTag at the same time. Please set gitTag to false')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a releaseStream attribute" () {
        setup:
        when:
            edgeXReleaseDocs.validate(validReleaseYaml.findAll {it.key != 'releaseStream'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDocs] Release yaml does not contain \'releaseStream\' (branch where you are releasing from). Example: main')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            edgeXReleaseDocs.validate(validReleaseYaml.findAll {it.key != 'version'})
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDocs] Release yaml does not contain release \'version\'. Example: 2.2.0')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.docsInfo = validReleaseYaml.docsInfo.findAll { it.key != 'nextReleaseName' }
            edgeXReleaseDocs.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDocs] Release yaml does not contain \'docsInfo.nextReleaseName\'. This is required to update the next version information. Example: levski')
    }

    def "Test validate [Should] raise error [When] release info yaml does not have a version attribute" () {
        setup:
        when:
            def validReleaseYamlCopy = validReleaseYaml.clone()
            validReleaseYamlCopy.docsInfo = validReleaseYaml.docsInfo.findAll { it.key != 'nextReleaseVersion' }
            edgeXReleaseDocs.validate(validReleaseYamlCopy)
        then:
            1 * getPipelineMock('error').call('[edgeXReleaseDocs] Release yaml does not contain release \'docsInfo.nextReleaseVersion\'. Example: 2.3.0')
    }

}