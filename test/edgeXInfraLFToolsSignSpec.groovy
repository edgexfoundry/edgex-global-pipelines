import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraLFToolsSignSpec extends JenkinsPipelineSpecification {

    def edgeXInfraLFToolsSign = null

    def setup() {

        edgeXInfraLFToolsSign = loadPipelineScriptForTest('vars/edgeXInfraLFToolsSign.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] sigulConfig is null" () {
        setup:
        when:
            edgeXInfraLFToolsSign([sigulConfig: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] sigulPassword is null" () {
        setup:
        when:
            edgeXInfraLFToolsSign([sigulPassword: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] sigulPKI is null" () {
        setup:
        when:
            edgeXInfraLFToolsSign([sigulPKI: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] directory is null and command is 'dir'" () {
        setup:
        when:
            edgeXInfraLFToolsSign([command: 'dir', directory: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] version is null and command is 'git-tag'" () {
        setup:
        when:
            edgeXInfraLFToolsSign([command: 'git-tag', version: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] command is null" () {
        setup:
        when:
            edgeXInfraLFToolsSign([command: null])
        then:
            thrown Exception
    }

    def "Test edgeXInfraLFToolsSign [Should] call expected shell scripts with expected arguments [When] command is 'dir'" () {
        setup:
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.31.0-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
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
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.31.0-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
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

    def "Test edgeXInfraLFToolsSign [Should] throw exception [When] command is invalid" () {
        setup:
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineVariable('docker')
            getPipelineMock('docker.image')('nexus3.edgexfoundry.org:10003/edgex-lftools:0.31.0-centos7') >> explicitlyMockPipelineVariable('DockerImageMock')
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
            edgeXInfraLFToolsSign([command: 'invalid command'])
        then:
            1 * getPipelineMock('sh').call([script:'sigul-configuration'])
            1 * getPipelineMock('sh').call('mkdir /home/jenkins && mkdir /home/jenkins/sigul')
            1 * getPipelineMock('sh').call('cp -R $HOME/sigul/* /home/jenkins/sigul/')
            1 * getPipelineMock('sh').call([script:'sigul-install'])
            1 * getPipelineMock('sh').call([script:'sigul-configuration-cleanup'])
            thrown Exception
    }

}
