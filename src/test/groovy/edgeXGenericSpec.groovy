import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXGenericSpec extends JenkinsPipelineSpecification {

    def edgeXGeneric = null

    def setup() {
        edgeXGeneric = loadPipelineScriptForTest('vars/edgeXGeneric.groovy')

        explicitlyMockPipelineVariable('out')
        explicitlyMockPipelineVariable('edgex')
    }

    def "Test validate [Should] raise error [When] config has no project" () {
        setup:
        when:
            edgeXGeneric.validate([:])
        then:
            1 * getPipelineMock('error').call('[edgeXGeneric] The parameter "project" is required. This is typically the project name.')
    }

    def "Test toEnvironment [Should] return expected [When] called - DD" () {
        setup:
            getPipelineMock('edgex.defaultFalse').call(null) >> false

        expect:
            edgeXGeneric.toEnvironment(config) == expectedResult

        where:
            config << [
                [
                    project: 'MyProject'
                ], [
                    project: 'MyProject',
                    env: []
                ], [
                    project: 'MyProject',
                    env: [
                        key1: 'value1',
                        key2: 'value2'
                    ]
                ], [
                    project: 'MyProject',
                    mavenSettings: [
                        'setting1:value1',
                        'setting2:value2',
                        'MyProject-settings:SETTINGS_FILE'
                    ]
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    EXTRA_SETTINGS: '',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    EXTRA_SETTINGS: '',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    EXTRA_SETTINGS: '',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                    key1: 'value1',
                    key2: 'value2'
                ], [
                    MAVEN_SETTINGS: 'MyProject-settings',
                    EXTRA_SETTINGS: 'setting1:value1,setting2:value2',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false
                ]
            ]
    }

    def "Test toEnvironment [Should] return expected [When] called for Sandbox - DD" () {
        setup:
            getPipelineMock('edgex.defaultFalse').call(null) >> false
            def environmentVariables = [
                'SILO': 'sandbox'
            ]
            edgeXGeneric.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeXGeneric.toEnvironment(config) == expectedResult

        where:
            config << [
                [
                    project: 'MyProject'
                ], [
                    project: 'MyProject',
                    mavenSettings: [
                        'setting1:value1',
                        'setting2:value2',
                        'MyProject-settings:SETTINGS_FILE'
                    ]
                ]
            ]
            expectedResult << [
                [
                    MAVEN_SETTINGS: 'sandbox-settings',
                    EXTRA_SETTINGS: '',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false,
                ], [
                    MAVEN_SETTINGS: 'sandbox-settings',
                    EXTRA_SETTINGS: 'setting1:value1,setting2:value2',
                    PROJECT: 'MyProject',
                    USE_SEMVER: false
                ]
            ]
    }

    def "Test getScripts [Should] return expected [When] called - DD" () {
        setup:
        expect:
            edgeXGeneric.getScripts(config, 'build_script', 'main') == expectedResult

        where:
            config << [
                [
                    branches: [
                        main: [
                            pre_build_script: '!include-raw-escape: shell/pre.sh',
                            build_script: 'make test verify && make build docker',
                            post_build_script: 'shell/post.sh'
                        ],
                        releaseA: [
                            pre_build_script: '!include-raw-escape: shell/pre.sh releaseA',
                            build_script: 'make test verify && make build docker',
                            post_build_script: 'shell/post.sh releaseA'
                        ]
                    ]
                ]
            ]
            expectedResult << [
                ['make test verify && make build docker']
            ]
    }

    def "Test allScripts [Should] return expected [When] called - DD" () {
        setup:
        expect:
            edgeXGeneric.allScripts(config, 'pre_build_script', 'main') == expectedResult

        where:
            config << [
                [
                    branches: [
                        main: [
                            pre_build_script: '!include-raw-escape: shell/pre.sh',
                            build_script: 'make test verify && make build docker',
                            post_build_script: 'shell/post.sh'
                        ],
                        releaseA: [
                            pre_build_script: '!include-raw-escape: shell/pre.sh releaseA',
                            build_script: 'make test verify && make build docker',
                            post_build_script: 'shell/post.sh releaseA'
                        ],
                        '*': [
                            pre_build_script: 'shell/all_pre.sh',
                            build_script: 'shell/all_build.sh',
                            post_build_script: 'shell/all_post.sh'
                        ]
                    ]
                ]
            ]
            expectedResult << [
                ['shell/all_pre.sh', '!include-raw-escape: shell/pre.sh']
            ]
    }

    def "Test anyScript [Should] return expected [When] called - DD" () {
        setup:
        expect:
            edgeXGeneric.anyScript(config, 'pre_build_script', 'main') == expectedResult

        where:
            config << [
                [
                    branches: [
                        main: [
                            pre_build_script: '!include-raw-escape: shell/pre.sh',
                            build_script: 'make test verify && make build docker',
                            post_build_script: 'shell/post.sh'
                        ]
                    ]
                ]
            ]
            expectedResult << [
                true
            ]
    }

    def "Test getConfigFilesFromEnv [Should] return expected [When] called" () {
        setup:
            def environmentVariables = [
                'EXTRA_SETTINGS': 'file1:var1,file2:var2,file3:var3'
            ]
            edgeXGeneric.getBinding().setVariable('env', environmentVariables)
            getPipelineMock('configFile.call')(['fileId':'file1', 'variable':'var1']) >> 'result1'
            getPipelineMock('configFile.call')(['fileId':'file2', 'variable':'var2']) >> 'result2'
            getPipelineMock('configFile.call')(['fileId':'file3', 'variable':'var3']) >> 'result3'
        expect:
            edgeXGeneric.getConfigFilesFromEnv() == ['result1', 'result2', 'result3']
    }

    def "Test setupPath [Should] return expected [When] called - DD" () {
        setup:
            def environmentVariables = [
                'PATH': '/usr/local/bin:/usr/bin:/bin'
            ]
            edgeXGeneric.getBinding().setVariable('env', environmentVariables)

        expect:
            edgeXGeneric.setupPath(config) == expectedResult

        where:
            config << [
                [
                    path: ['/opt/app1/bin', '/opt/app2/bin']
                ]
            ]

            expectedResult << [
                '/usr/local/bin:/usr/bin:/bin:/opt/app1/bin:/opt/app2/bin'
            ]
    }

}
