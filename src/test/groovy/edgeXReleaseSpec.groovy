import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseSpec extends JenkinsPipelineSpecification {
    
    def edgeXRelease = null

    def setup() {

        edgeXRelease = loadPipelineScriptForTest('vars/edgeXRelease.groovy')
        explicitlyMockPipelineVariable('out')
        edgeXRelease.getBinding().setVariable('edgex', {})
        explicitlyMockPipelineStep('didChange')
    }

    def "Test collectReleaseYamlFiles [Should] return instance of java.util.ArrayList of size 2 [When] called with no parameters and two changed files" () {
        setup:
            getPipelineMock('findFiles').call([glob:'release/*.yaml']) >> ['app-functions-sdk-go.yaml', 'edgex-go.yaml']
            getPipelineMock('didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('didChange').call('edgex-go.yaml', 'release') >> true
            getPipelineMock('readYaml').call([file:'app-functions-sdk-go.yaml']) >> [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
            getPipelineMock('readYaml').call([file:'edgex-go.yaml']) >> [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        
        expect:
            edgeXRelease.collectReleaseYamlFiles().getClass() == java.util.ArrayList
            edgeXRelease.collectReleaseYamlFiles().size() == 2
            edgeXRelease.collectReleaseYamlFiles() == [[
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ],
                [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]]
    }

    def "Test collectReleaseYamlFiles [Should] return instance of java.util.ArrayList of size 1 [When] called with no parameters and one changed file" () {
        setup:
            getPipelineMock('findFiles').call([glob:'release/*.yaml']) >> ['app-functions-sdk-go.yaml', 'edgex-go.yaml']
            getPipelineMock('didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('didChange').call('edgex-go.yaml', 'release') >> false
            getPipelineMock('readYaml').call([file:'app-functions-sdk-go.yaml']) >> [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
            getPipelineMock('readYaml').call([file:'edgex-go.yaml']) >> [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        
        expect:
            edgeXRelease.collectReleaseYamlFiles().getClass() == java.util.ArrayList
            edgeXRelease.collectReleaseYamlFiles().size() == 1
            edgeXRelease.collectReleaseYamlFiles() == [[
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]]
    }

    def "Test collectReleaseYamlFiles [Should] return instance of java.util.ArrayList of size 0 [When] called with no parameters and no changed files" () {
        setup:
            getPipelineMock('findFiles').call([glob:'release/*.yaml']) >> ['app-functions-sdk-go.yaml', 'edgex-go.yaml']
            getPipelineMock('didChange').call('app-functions-sdk-go.yaml', 'release') >> false
            getPipelineMock('didChange').call('edgex-go.yaml', 'release') >> false
            getPipelineMock('readYaml').call([file:'app-functions-sdk-go.yaml']) >> [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
            getPipelineMock('readYaml').call([file:'edgex-go.yaml']) >> [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        
        expect:
            edgeXRelease.collectReleaseYamlFiles().getClass() == java.util.ArrayList
            edgeXRelease.collectReleaseYamlFiles().size() == 0
            edgeXRelease.collectReleaseYamlFiles() == []
    }

    def "Test parallelStepFactory [Should] return a java.util.LinkedHashMap with keys [When] called" () {
        setup:
            def releaseData = [
                [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ],
                [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
            ]

        expect:
            edgeXRelease.parallelStepFactory(releaseData).getClass() == java.util.LinkedHashMap
            edgeXRelease.parallelStepFactory(releaseData).keySet().toString() == "[app-functions-sdk-go, edgex-go]"
    }

    def "Test collectReleaseYamlFiles [Should] return instance of java.util.ArrayList of size 2 [When] called with a filepath parameter and two changed files" () {
        setup:
            getPipelineMock('findFiles').call([glob:'fuji/*.yaml']) >> ['app-functions-sdk-go.yaml', 'edgex-go.yaml']
            getPipelineMock('didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('didChange').call('edgex-go.yaml', 'release') >> true
            getPipelineMock('readYaml').call([file:'app-functions-sdk-go.yaml']) >> [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
            getPipelineMock('readYaml').call([file:'edgex-go.yaml']) >> [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        
        expect:
            edgeXRelease.collectReleaseYamlFiles('fuji/*.yaml').getClass() == java.util.ArrayList
            edgeXRelease.collectReleaseYamlFiles('fuji/*.yaml').size() == 2
            edgeXRelease.collectReleaseYamlFiles('fuji/*.yaml') == [[
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ],
                [
                    name:'edgex-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/edgex-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/edgex-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]]
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseGitTag [When] called with gitTag: true" () {
        setup:
            explicitlyMockPipelineStep('println')
            explicitlyMockPipelineStep('edgeXReleaseGitTag')

            def step = 
                [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()

        then:
            1 * getPipelineMock("edgeXReleaseGitTag").call(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':true, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':false, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':false, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseSnap [When] called with snap: true" () {
        setup:
            explicitlyMockPipelineStep('println')
            explicitlyMockPipelineStep('edgeXReleaseSnap')

            def step = 
                [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:false, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:false, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:true, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()

        then:
            1 * getPipelineMock("edgeXReleaseSnap").call(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':false, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':false, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':true, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseDockerHub and edgeXReleaseDockerNexus [When] called with dockerImages: true" () {
        setup:
            explicitlyMockPipelineStep('println')
            explicitlyMockPipelineStep('edgeXReleaseDockerHub')
            explicitlyMockPipelineStep('edgeXReleaseDockerNexus')
            explicitlyMockPipelineStep('edgeXReleaseGitTag')

            def step = 
                [
                    name:'app-functions-sdk-go', 
                    version:'v1.2.0', 
                    releaseName:'geneva', 
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    gitTag:true, 
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git', 
                    dockerImages:true, 
                    dockerSource:['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 
                    dockerDestination:['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 
                    snap:false, 
                    snapDestination:'https://snapcraft.org/..', 
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()

        then:
            1 * getPipelineMock("edgeXReleaseDockerHub").call(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':true, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':true, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':false, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
            1 * getPipelineMock("edgeXReleaseGitTag").call(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':true, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':true, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':false, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
            1 * getPipelineMock("edgeXReleaseDockerNexus").call(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':true, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':true, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':false, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
    }
}