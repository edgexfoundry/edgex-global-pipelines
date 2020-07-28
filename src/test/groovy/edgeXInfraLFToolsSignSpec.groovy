import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraLFToolsSignSpec extends JenkinsPipelineSpecification {

    def edgeXInfraLFToolsSign = null

    public static class TestException extends RuntimeException {
        public TestException(String _message) {
            super( _message );
        }
    }

    def setup() {

        edgeXInfraLFToolsSign = loadPipelineScriptForTest('vars/edgeXInfraLFToolsSign.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXInfraLFToolsSign [Should] raise error [When] directory is null and command is 'dir'" () {
        setup:
            // NOTE - docker still needs to be stubbed because error below is caught but execution will continue
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.23.1-centos7') >> explicitlyMockPipelineVariable()
        when:
            try {
                edgeXInfraLFToolsSign([command: 'dir', directory: null])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('Directory location (directory) is required to sign files in a directory.')
    }

    def "Test edgeXInfraLFToolsSign [Should] raise error [When] version is null and command is 'git-tag'" () {
        setup:
            // NOTE - docker still needs to be stubbed because error below is caught but execution will continue
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.23.1-centos7') >> explicitlyMockPipelineVariable()
        when:
            try {
                edgeXInfraLFToolsSign([command: 'git-tag', version: null])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call('Version number (version) is required to sign a git teg.')
    }

    def "Test edgeXInfraLFToolsSign [Should] raise error [When] command is null" () {
        setup:
        when:
            try {
                edgeXInfraLFToolsSign([command: ''])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call("Invalid command (command: ) provided for the edgeXInfraLFToolsSign function. (Valid values: dir, git-tag)")
    }

    def "Test edgeXInfraLFToolsSign [Should] call expected shell scripts with expected arguments [When] command is 'dir'" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.23.1-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration.sh') >> {
                return 'sigul-configuration'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-install.sh') >> {
                return 'sigul-install'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration-cleanup.sh') >> {
                return 'sigul-configuration-cleanup'
            }
        when:
            edgeXInfraLFToolsSign([command: 'dir', directory: 'MyDirectory'])
        then:
            1 * getPipelineMock('sh').call([script:'sigul-configuration'])
            1 * getPipelineMock('sh').call('mkdir /home/jenkins && mkdir /home/jenkins/sigul')
            1 * getPipelineMock('sh').call('cp -R $HOME/sigul/* /home/jenkins/sigul/')
            1 * getPipelineMock('sh').call([script:'sigul-install'])
            1 * getPipelineMock('sh').call("lftools sign sigul -m \"parallel\" \"MyDirectory\"")
            1 * getPipelineMock('sh').call("chmod -R 775 MyDirectory && chown -R 1001:1001 MyDirectory")
            1 * getPipelineMock('sh').call([script:'sigul-configuration-cleanup'])
    }

    def "Test edgeXInfraLFToolsSign [Should] call expected shell scripts with expected arguments [When] command is 'git-tag'" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.23.1-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration.sh') >> {
                return 'sigul-configuration'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-install.sh') >> {
                return 'sigul-install'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration-cleanup.sh') >> {
                return 'sigul-configuration-cleanup'
            }
        when:
            edgeXInfraLFToolsSign([command: 'git-tag', directory: 'MyDirectory', version: 'MyVersion'])
        then:
            1 * getPipelineMock('sh').call([script:'sigul-configuration'])
            1 * getPipelineMock('sh').call('mkdir /home/jenkins && mkdir /home/jenkins/sigul')
            1 * getPipelineMock('sh').call('cp -R $HOME/sigul/* /home/jenkins/sigul/')
            1 * getPipelineMock('sh').call([script:'sigul-install'])
            1 * getPipelineMock('sh').call('git tag --list')
            1 * getPipelineMock('sh').call("lftools sign git-tag MyVersion")
            1 * getPipelineMock('sh').call([script:'sigul-configuration-cleanup'])
    }

    def "Test edgeXInfraLFToolsSign [Should] raise error [When] command is invalid" () {
        setup:
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.23.1-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration.sh') >> {
                return 'sigul-configuration'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-install.sh') >> {
                return 'sigul-install'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/sigul-configuration-cleanup.sh') >> {
                return 'sigul-configuration-cleanup'
            }
        when:
            try {
                edgeXInfraLFToolsSign([command: 'invalid command'])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('sh').call([script:'sigul-configuration'])
            1 * getPipelineMock('sh').call('mkdir /home/jenkins && mkdir /home/jenkins/sigul')
            1 * getPipelineMock('sh').call('cp -R $HOME/sigul/* /home/jenkins/sigul/')
            1 * getPipelineMock('sh').call([script:'sigul-install'])
            2 * getPipelineMock('sh').call([script:'sigul-configuration-cleanup'])
            1 * getPipelineMock('error').call("Invalid command (command: invalid command) provided for the edgeXInfraLFToolsSign function. (Valid values: dir, git-tag)")
    }

}
