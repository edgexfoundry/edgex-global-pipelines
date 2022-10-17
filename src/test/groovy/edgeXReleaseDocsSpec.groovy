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
                reviewers:'mock-reviewers'
            ]
        ]
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
            
            getPipelineMock('sh')("jq 'map((select(.aliases[0] == \"latest\") | .aliases) |= [])' docs/versions.json | tee docs/versions.1") >> '''
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
                }
            ]
            '''.stripIndent()

            getPipelineMock('sh')("jq 'map((select(.version == \"2.3\") | .aliases) |= [\"latest\"])' docs/versions.1 | tee docs/versions.2") >> '''
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
                    "aliases": ["latest"]
                }
            ]
            '''.stripIndent()

            getPipelineMock('sh')([script: "jq '. += [{\"version\": \"2.3\", \"title\": \"2.3-NextRelease\", \"aliases\": []}]' docs/versions.2", returnStdout: true]) >> '''
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
                    "aliases": ["latest"]
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

            1 * getPipelineMock('sh').call("sed -i 's|2.2|2.3|g' mkdocs.yml")

            1 * getPipelineMock('writeFile').call(['file': 'docs/versions.json',  'text':'\n[ {     "version": "2.0",     "title": "2.0-Ireland",     "aliases": [] }, {     "version": "2.1",     "title": "2.1-Jakarta",     "aliases": [] }, {     "version": "2.2",     "title": "2.2-Kamakura",     "aliases": ["latest"] }, {     "version": "2.3",     "title": "2.3-NextRelease",     "aliases": [] }\n]\n'])
            1 * getPipelineMock('writeFile').call(['file': 'template_macros.yaml',  'text':"latest_released_version: '2.2.0'\nlatest_release_name: 'currentRelease'\nnext_version: '2.3.0'\n"])

            1 * getPipelineMock('sh').call('git diff')

            1 * getPipelineMock("edgex.createPR").call(['currentRelease-version-changes', 'ci: automated version file changes for [2.3]', 'This PR updates the version files to the next release version 2.3', 'mock-reviewers'])
    }

    def "Test createVersionMacroFile [Should] execute expected [When] called" () {
        expect:
            edgeXReleaseDocs.createVersionMacroFile(releaseVersion, releaseName, nextVersion) == expectedResult
        where:
            releaseVersion << [
                '1.5.0',
                '2.2.0'
            ]
            releaseName << [
                'mock-release',
                'kamakura'
            ]
            nextVersion << [
                '1.6.0',
                '2.3.0'
            ]
            expectedResult << [
                "latest_released_version: '1.5.0'\nlatest_release_name: 'mock-release'\nnext_version: '1.6.0'\n",
                "latest_released_version: '2.2.0'\nlatest_release_name: 'kamakura'\nnext_version: '2.3.0'\n",
            ]
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