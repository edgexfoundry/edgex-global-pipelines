import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXSemverSpec extends JenkinsPipelineSpecification {

    def edgeXSemver = null

    def setup() {
        edgeXSemver = loadPipelineScriptForTest('vars/edgeXSemver.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXSemver [Should] call expected set VERSION [When] no command" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver()
        then:
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            1 * getPipelineMock('sh')([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
            environmentVariables['VERSION'] == '1.2.3-dev.4'
    }

    def "Test edgeXSemver [Should] return expected [When] no command" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
        expect:
            def result = edgeXSemver()
            result == '1.2.3-dev.4'
    }

    def "Test edgeXSemver [Should] call expected set VERSION and set GITSEMVER_INIT_VERSION [When] command is init with semverVersion and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver('init', '1.2.4')
        then:
            // verify docker.image
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable('DockerImageMock')
            // verify docker.image.inside arguments
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '-u 0:0 -v /etc/ssh:/etc/ssh'
                assert dockerArgs == _arguments[0][0]
            }
            // verify withEnv envvars
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SSH_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts',
                    'SEMVER_DEBUG=on'
                ]
                assert envArgs == _arguments[0][0]
            }
            // verify git semver command
            1 * getPipelineMock('sh').call('git semver init -ver=1.2.4 -force')
            environmentVariables['VERSION'] == '1.2.4'
            // verify writefile
            1 * getPipelineMock('writeFile').call([file: 'VERSION', text: '1.2.4'])
            // should not call get tag to determine if HEAD is tagged since GITSEMVER_HEAD_TAG is set
            0 * getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true])
            environmentVariables['GITSEMVER_INIT_VERSION'] == '1.2.4'
    }

    def "Test edgeXSemver [Should] call expected and set VERSION [When] called with init command and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'MyTag'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver('init')
        then:
            1 * getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()

            1 * getPipelineMock('echo')("[edgeXSemver]: GITSEMVER_HEAD_TAG is already set to 'MyTag'")

            1 * getPipelineMock('sh').call('git semver init')

            1 * getPipelineMock('sh').call([script: 'git semver', returnStdout: true]) >> '1.2.3-dev.4\n'
            environmentVariables['VERSION'] == '1.2.3-dev.4'
    }

    def "Test edgeXSemver [Should] call command and set GITSEMVER_HEAD_TAG [When] command is init without semverVersion and HEAD is tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> 'v1.2.3'
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('init')
        then:
            1 * getPipelineMock('echo')("[edgeXSemver]: set GITSEMVER_HEAD_TAG to 'v1.2.3'")
            1 * getPipelineMock('sh').call('git semver init')
            environmentVariables['GITSEMVER_HEAD_TAG'] == 'v1.2.3'
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] call command and not set GITSEMVER_HEAD_TAG [When] command is init without semverVersion and HEAD is not tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> ''
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('init')
        then:
            1 * getPipelineMock('sh').call('git semver init')
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
            environmentVariables['VERSION'] == '1.2.4-dev.1'
            environmentVariables['GITSEMVER_INIT_VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] call command and set GITSEMVER_HEAD_TAG [When] command is init with semverVersion and HEAD is tagged with semverVersion" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> 'v2.3.4\nstable'
        when:
            edgeXSemver('init', '2.3.4')
        then:
            1 * getPipelineMock('sh').call('git semver init -ver=2.3.4 -force')
            environmentVariables['GITSEMVER_HEAD_TAG'] == 'v2.3.4|stable'
            environmentVariables['VERSION'] == '2.3.4'
            environmentVariables['GITSEMVER_INIT_VERSION'] == '2.3.4'
    }

    def "Test edgeXSemver [Should] call command and not set GITSEMVER_HEAD_TAG [When] command is init with semverVersion and HEAD is not tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> ''
        when:
            edgeXSemver('init', '2.3.4')
        then:
            1 * getPipelineMock('sh').call('git semver init -ver=2.3.4 -force')
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
            environmentVariables['VERSION'] == '2.3.4'
            environmentVariables['GITSEMVER_INIT_VERSION'] == '2.3.4'
    }

    def "Test edgeXSemver [Should] call command and not set GITSEMVER_HEAD_TAG [When] command is init with semverVersion and HEAD is not tagged with semverVersion" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> 'v2.3.4-dev.13'
        when:
            edgeXSemver('init', '2.3.4')
        then:
            1 * getPipelineMock('sh').call('git semver init -ver=2.3.4 -force')
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
            environmentVariables['VERSION'] == '2.3.4'
            environmentVariables['GITSEMVER_INIT_VERSION'] == '2.3.4'
    }

    def "Test edgeXSemver [Should] call command and not set GITSEMVER_HEAD_TAG [When] command is init ver force and not semverVersion" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            // TODO: I'm not convinced this should be supported - specify semverVersion if specific version is to be specified
            edgeXSemver('init -ver=2.3.4 -force')
        then:
            0 * getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true)
            1 * getPipelineMock('sh').call('git semver init -ver=2.3.4 -force')
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] not call command [When] command is tag and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': '1.2.3'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('tag')
        then:
            1 * getPipelineMock('echo')("[edgeXSemver]: ignoring command tag because GITSEMVER_HEAD_TAG is already set to '1.2.3'")
            0 * getPipelineMock('sh').call('git semver tag')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] call command [When] command is tag and GITSEMVER_HEAD_TAG is not set" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> ''
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('tag')
        then:
            1 * getPipelineMock('sh').call('git semver tag')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] call command [When] command is tag force and GITSEMVER_HEAD_TAG is not set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_INIT_VERSION': '1.2.3'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git tag --points-at HEAD', returnStdout: true) >> ''
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.3'
        when:
            edgeXSemver('tag -force')
        then:
            0 * getPipelineMock('echo')("[edgeXSemver]: HEAD is already tagged with v1.2.3")
            1 * getPipelineMock('echo')("[edgeXSemver]: removing remote and local tags for v1.2.3")
            1 * getPipelineMock('sh').call('git semver tag -force')
            environmentVariables['VERSION'] == '1.2.3'
    }

    def "Test edgeXSemver [Should] call command [When] command is bump and GITSEMVER_HEAD_TAG is not set" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('bump pre')
        then:
            1 * getPipelineMock('sh').call('git semver bump pre')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] not call command [When] command is bump and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': '1.2.4-dev.1'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('bump pre')
        then:
            1 * getPipelineMock('echo')("[edgeXSemver]: ignoring command bump pre because GITSEMVER_HEAD_TAG is already set to '1.2.4-dev.1'")
            0 * getPipelineMock('sh').call('git semver bump pre')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] call command [When] command is push and GITSEMVER_HEAD_TAG is not set" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('push')
        then:
            1 * getPipelineMock('sh').call('git semver push')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test edgeXSemver [Should] not call command [When] command is push and GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': '1.2.4-dev.1'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10004/edgex-devops/git-semver:latest') >> explicitlyMockPipelineVariable()
            getPipelineMock('sh')(script: 'git semver', returnStdout: true) >> '1.2.4-dev.1'
        when:
            edgeXSemver('push')
        then:
            1 * getPipelineMock('echo')("[edgeXSemver]: ignoring command push because GITSEMVER_HEAD_TAG is already set to '1.2.4-dev.1'")
            0 * getPipelineMock('sh').call('git semver push')
            environmentVariables['VERSION'] == '1.2.4-dev.1'
    }

    def "Test executeGitSemver [Should] call sshagent and sh with the expected arguments [When] called" () {
        setup:
        when:
            edgeXSemver.executeGitSemver('MyCredentials', 'git semver bump pre')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call('git semver bump pre')
    }

    def "Test executeGitSemver [Should] delete remote and local tag and call command [When] tag force and HEAD is not tagged" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_INIT_VERSION': '1.2.3'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> ''
        when:
            edgeXSemver.executeGitSemver('MyCredentials', 'git semver tag -force')
        then:
            // ugly to assert due to spaces after carriage returns
            // 1 * getPipelineMock('sh').call('set +x\nset +e\ngit push origin :refs/tags/v1.2.3\ngit tag -d v1.2.3\nset -e\n')
            1 * getPipelineMock('echo').call("[edgeXSemver]: removing remote and local tags for v1.2.3")
            1 * getPipelineMock('sh').call('git semver tag -force')
    }

    def "Test setGitSemverHeadTag [Should] not call sshagent [When] GITSEMVER_HEAD_TAG is set" () {
        setup:
            def environmentVariables = [
                'GITSEMVER_HEAD_TAG': 'v1.2.3-dev.1'
            ]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver.setGitSemverHeadTag('', 'MyCredentials')
        then:
            0 * getPipelineMock('sshagent').call(_)
            1 * getPipelineMock('echo')("[edgeXSemver]: GITSEMVER_HEAD_TAG is already set to 'v1.2.3-dev.1'")
    }

    def "Test setGitSemverHeadTag [Should] set GITSEMVER_HEAD_TAG [When] no init version and HEAD is tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
        when:
            edgeXSemver.setGitSemverHeadTag('', 'MyCredentials')
        then:
            1 * getPipelineMock('sshagent').call(_) >> { _arguments ->
                assert ['credentials':['MyCredentials']] == _arguments[0][0]
            }
            1 * getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> 'v1.0.3\nexperimental'
            environmentVariables['GITSEMVER_HEAD_TAG'] == 'v1.0.3|experimental'
    }

    def "Test setGitSemverHeadTag [Should] not set GITSEMVER_HEAD_TAG [When] no init version and HEAD is not tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> ''
        when:
            edgeXSemver.setGitSemverHeadTag('', 'MyCredentials')
        then:
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
    }

    def "Test setGitSemverHeadTag [Should] set GITSEMVER_HEAD_TAG [When] init version and HEAD is tagged with init version" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> 'v1.2.3\nexperimental'
        when:
            edgeXSemver.setGitSemverHeadTag('1.2.3', 'MyCredentials')
        then:
            1 * getPipelineMock('echo')("[edgeXSemver]: HEAD is already tagged with v1.2.3")
            1 * getPipelineMock('echo')("[edgeXSemver]: set GITSEMVER_HEAD_TAG to 'v1.2.3|experimental'")
            environmentVariables['GITSEMVER_HEAD_TAG'] == 'v1.2.3|experimental'
    }

    def "Test setGitSemverHeadTag [Should] not set GITSEMVER_HEAD_TAG [When] init version and HEAD is not tagged with init version" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> 'v1.2.3-dev.13'
        when:
            edgeXSemver.setGitSemverHeadTag('1.2.3', 'MyCredentials')
        then:
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
    }

    def "Test setGitSemverHeadTag [Should] not set GITSEMVER_HEAD_TAG [When] init version and HEAD is not tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> ''
        when:
            edgeXSemver.setGitSemverHeadTag('1.2.3', 'MyCredentials')
        then:
            environmentVariables.containsKey('GITSEMVER_HEAD_TAG') == false
    }

    def "Test getHeadTags [Should] return expected list [When] HEAD is tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> 'tag1\ntag2\ntag3'
        expect:
            edgeXSemver.getHeadTags('MyCredentials') == ['tag1', 'tag2', 'tag3']
    }

    def "Test getHeadTags [Should] return empty list [When] HEAD is not tagged" () {
        setup:
            def environmentVariables = [:]
            edgeXSemver.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('sh')([script:'git tag --points-at HEAD', returnStdout:true]) >> ''
        expect:
            edgeXSemver.getHeadTags('MyCredentials') == []
    }
}
