import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXReleaseSpec extends JenkinsPipelineSpecification {

    def edgeXRelease = null

    def setup() {

        edgeXRelease = loadPipelineScriptForTest('vars/edgeXRelease.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineStep('build')

        explicitlyMockPipelineVariable('edgex')
        explicitlyMockPipelineVariable('edgeXLTS')
        explicitlyMockPipelineVariable('edgeXReleaseSnap')
        explicitlyMockPipelineVariable('edgeXReleaseGitTag')
        explicitlyMockPipelineVariable('edgeXReleaseDockerImage')
        explicitlyMockPipelineVariable('edgeXReleaseGitHubAssets')
    }

    def "Test collectReleaseYamlFiles [Should] return instance of java.util.ArrayList of size 2 [When] called with no parameters and two changed files" () {
        setup:
            getPipelineMock('findFiles').call([glob:'release/*.yaml']) >> ['app-functions-sdk-go.yaml', 'edgex-go.yaml']
            getPipelineMock('edgex.didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('edgex.didChange').call('edgex-go.yaml', 'release') >> true
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
            getPipelineMock('edgex.didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('edgex.didChange').call('edgex-go.yaml', 'release') >> false
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
            getPipelineMock('edgex.didChange').call('app-functions-sdk-go.yaml', 'release') >> false
            getPipelineMock('edgex.didChange').call('edgex-go.yaml', 'release') >> false
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
            getPipelineMock('edgex.didChange').call('app-functions-sdk-go.yaml', 'release') >> true
            getPipelineMock('edgex.didChange').call('edgex-go.yaml', 'release') >> true
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
            1 * getPipelineMock("edgeXReleaseGitTag.call")(
                ['name':'app-functions-sdk-go',
                'version':'v1.2.0',
                'releaseName':'geneva',
                'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                'gitTag':true,
                'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                'dockerImages':false,
                'dockerSource':['https://nexus3.edgexfoundry.org/..',
                'https://nexus3.edgexfoundry.org/..'],
                'dockerDestination':['https://nexus3.edgexfoundry.org/..',
                'https://hub.docker.com/..'],
                'snap':false,
                'snapDestination':'https://snapcraft.org/..',
                'snapChannel':'geneva'] as Map,
                ['credentials':'edgex-jenkins-ssh','bump':false,'tag':true] as Map
                )
            1 * getPipelineMock("edgeXReleaseGitTag.call")(
                ['name':'app-functions-sdk-go',
                'version':'v1.2.0',
                'releaseName':'geneva',
                'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                'gitTag':true,
                'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                'dockerImages':false,
                'dockerSource':['https://nexus3.edgexfoundry.org/..',
                'https://nexus3.edgexfoundry.org/..'],
                'dockerDestination':['https://nexus3.edgexfoundry.org/..',
                'https://hub.docker.com/..'],
                'snap':false,
                'snapDestination':'https://snapcraft.org/..',
                'snapChannel':'geneva'] as Map,
                ['credentials':'edgex-jenkins-ssh','bump':true,'tag':false] as Map
                )
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseSnap [When] called with snap: true" () {
        setup:
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
            0 * getPipelineMock("edgeXReleaseSnap.call")(['name':'app-functions-sdk-go', 'version':'v1.2.0', 'releaseName':'geneva', 'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'gitTag':false, 'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git', 'dockerImages':false, 'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'], 'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'], 'snap':true, 'snapDestination':'https://snapcraft.org/..', 'snapChannel':'geneva'])
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseDockerImage [When] called with dockerImages: true" () {
        setup:
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.2.0',
                    releaseName:'geneva',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:false,
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    dockerImages:true,
                    dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:main'],
                    dockerDestination:['nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go', 'edgexfoundry/docker-app-functions-sdk-go'],
                    snap:false,
                    snapDestination:'https://snapcraft.org/..',
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()

        then:
            println("No assertions yet.")
    }

    def "Test parallelStepFactoryTransform [Should] call edgeXReleaseGitHubAssets [When] called with gitHubRelease: true" () {
        setup:
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
                    snap:false,
                    snapDestination:'https://snapcraft.org/..',
                    snapChannel:'geneva',
                    gitHubRelease:true,
                    gitHubReleaseAssets:['https://nexus-location/asset1', 'https://nexus-location/asset2']
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()

        then:
            1 * getPipelineMock("edgeXReleaseGitHubAssets.call")(
                [
                    'name':'app-functions-sdk-go',
                    'version':'v1.2.0',
                    'releaseName':'geneva',
                    'repo':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    'gitTag':false,
                    'gitTagDestination':'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    'dockerImages':false,
                    'dockerSource':['https://nexus3.edgexfoundry.org/..', 'https://nexus3.edgexfoundry.org/..'],
                    'dockerDestination':['https://nexus3.edgexfoundry.org/..', 'https://hub.docker.com/..'],
                    'snap':false,
                    'snapDestination':'https://snapcraft.org/..',
                    'snapChannel':'geneva',
                    'gitHubRelease':true,
                    'gitHubReleaseAssets':['https://nexus-location/asset1', 'https://nexus-location/asset2']
                ])
    }

    def "Test stageArtifact [Should] call expected [When] DRY_RUN is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> true
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.2.0',
                    releaseName:'geneva',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:false,
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    dockerImages:true,
                    dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:main'],
                    dockerDestination:['nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go', 'edgexfoundry/docker-app-functions-sdk-go'],
                    snap:false,
                    snapDestination:'https://snapcraft.org/..',
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.stageArtifact(step)
        then:
            0 * getPipelineMock("build").call(_)
    }

    def "Test stageArtifact [Should] call expected [When] DRY_RUN is false" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.2.0',
                    releaseName:'geneva',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:false,
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    dockerImages:true,
                    dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:main'],
                    dockerDestination:['nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go', 'edgexfoundry/docker-app-functions-sdk-go'],
                    snap:false,
                    snapDestination:'https://snapcraft.org/..',
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.stageArtifact(step)
        then:
            1 * getPipelineMock("build").call(["job": "../app-functions-sdk-go/main", "parameters": [[$class: 'StringParameterValue', name: 'CommitId', value: '0123456789']], "propagate": true, "wait": true])
    }

    def "Test stageArtifact [Should] call expected [When] build item not found" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('build').call(_) >> {
                throw new hudson.AbortException('No item named app-functions-sdks-go/main found')
            }
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.31.0',
                    releaseName:'geneva',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:true,
                    dockerImages:false
                ]
        when:
            edgeXRelease.stageArtifact(step)
        then:
            1 * getPipelineMock("error").call('[edgeXRelease]: No build pipeline found - No artifact to stage')
            notThrown hudson.AbortException
    }

    def "Test stageArtifact [Should] throw AbortException [When] AbortException is not caused by lack of Jenkinsfile" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('build').call(_) >> {
                throw new hudson.AbortException('Any Other AbortException exception')
            }
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.31.0',
                    releaseName:'geneva',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:true,
                    dockerImages:true,
                    dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:main'],
                    dockerDestination:['nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go', 'edgexfoundry/docker-app-functions-sdk-go']
                ]
        when:
            edgeXRelease.stageArtifact(step)
        then:
            0 * getPipelineMock("echo").call('[edgeXRelease]: No build pipeline found - no artifact to stage')
            thrown hudson.AbortException
    }

    def "Test stageArtifact [Should] call expected [When] DRY_RUN is false and LTS is true" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            def step =
                [
                    name:'app-functions-sdk-go',
                    version:'v1.2.0',
                    releaseName:'lts-release',
                    releaseStream:'main',
                    lts: true,
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    gitTag:false,
                    gitTagDestination:'https://github.com/edgexfoundry/app-functions-sdk-go.git',
                    dockerImages:true,
                    dockerSource:['nexus3.edgexfoundry.org:10004/docker-app-functions-sdk-go:main'],
                    dockerDestination:['nexus3.edgexfoundry.org:10002/docker-app-functions-sdk-go', 'edgexfoundry/docker-app-functions-sdk-go'],
                    snap:false,
                    snapDestination:'https://snapcraft.org/..',
                    snapChannel:'geneva'
                ]
        when:
            edgeXRelease.stageArtifact(step)
        then:
            1 * getPipelineMock("build").call(["job": "../app-functions-sdk-go/lts-release", "parameters": [[$class: 'StringParameterValue', name: 'CommitId', value: '0123456789']], "propagate": true, "wait": true])
    }

    def "Test parallelStepFactoryTransform [Should Not] wait for builder images [When] called with Go-based docker image" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('edgeXLTS.prepGoProject').call(_) >> true
            def step =
                [
                    lts:true,
                    name:'sample-service',
                    version:'1.11.83',
                    releaseName:'jakarta',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service',
                            'docker.io/edgexfoundry/sample-service'
                        ]
                    ], [
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()
        then:
            0 * getPipelineMock('edgex.waitForImages').call(_)
    }
    
    def "Test parallelStepFactoryTransform [Should Not] setup LTS branch [When] lts:false is set in release yaml." () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('edgeXLTS.prepGoProject').call(_) >> true
            def step =
                [
                    lts:false,
                    name:'sample-service',
                    version:'1.11.83',
                    releaseName:'jakarta',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service',
                            'docker.io/edgexfoundry/sample-service'
                        ]
                    ], [
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()
        then:
            0 * getPipelineMock('edgeXLTS.prepLTS').call(_)
    }
    
    def "Test parallelStepFactoryTransform [Should] setup LTS branch [When] lts:true is set in release yaml." () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            getPipelineMock('edgeXLTS.prepGoProject').call(_) >> true
            def step =
                [
                    lts:true,
                    name:'sample-service',
                    version:'1.11.83',
                    releaseName:'jakarta',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service',
                            'docker.io/edgexfoundry/sample-service'
                        ]
                    ], [
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            edgeXRelease.parallelStepFactoryTransform(step).asWritable().toString()
        then:
            1 * getPipelineMock('edgeXLTS.prepLTS').call(_)
    }

    def "Test getBuilderImagesFromReleasedImages [Should] return expected [When] called with C-based docker image and both architectures" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            def step =
                [
                    name:'sample-service-c',
                    version:'1.11.83',
                    releaseName:'jakarta',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service',
                            'docker.io/edgexfoundry/sample-service'
                        ]
                    ], [
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            def images = edgeXRelease.getBuilderImagesFromReleasedImages(step, 'MockTag')
        then:
            assert images == ["nexus3.edgexfoundry.org:10002/sample-service-c-builder-x86_64:MockTag",
            "nexus3.edgexfoundry.org:10002/sample-service-c-builder-arm64:MockTag"]
    }

    def "Test getBuilderImagesFromReleasedImages [Should] return expected [When] called with C-based docker image and only ARM architecture" () {
        setup:
            getPipelineMock('edgex.isDryRun').call() >> false
            def step =
                [
                    name:'sample-service-c',
                    version:'1.11.83',
                    releaseName:'lts-test',
                    releaseStream:'main',
                    commitId:'0123456789',
                    repo:'https://github.com/edgexfoundry/sample-service.git',
                    gitTag:false,
                    dockerImages:false,
                    docker:[[
                        image: 'nexus3.edgexfoundry.org:10004/sample-service-arm64',
                        destination: [
                            'nexus3.edgexfoundry.org:10002/sample-service-arm64',
                            'docker.io/edgexfoundry/sample-service-arm64'
                        ]
                    ]]
                ]
        when:
            def images = edgeXRelease.getBuilderImagesFromReleasedImages(step, '9f8c2f471')
        then:
            assert images == ["nexus3.edgexfoundry.org:10002/sample-service-c-builder-arm64:9f8c2f471"]
    }
}