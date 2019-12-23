import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXGenericSpec extends JenkinsPipelineSpecification {

    def edgeXGeneric = null
    def environment = [:]

    public static class TestException extends RuntimeException {
        public TestException(String _message) { 
            super( _message );
        }
    }

    def setup() {
        edgeXGeneric = loadPipelineScriptForTest('vars/edgeXGeneric.groovy')
        edgeXGeneric.getBinding().setVariable('env', environment)
        explicitlyMockPipelineVariable('out')
    }

    def "Test validate [Should] raise error [When] config has no project" () {
        setup:
            explicitlyMockPipelineStep('error')
        when:
            try {
                edgeXGeneric.validate([:])
            }
            catch(TestException exception) {
            }
        then:
            1 * getPipelineMock('error').call(_ as String)
    }

    def "Test toEnvironment [Should] return expected [When] called - DD" () {
        setup:
            getPipelineMock('edgex.defaultFalse')(null) >> {
                false
            }

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
            getPipelineMock('edgex.defaultFalse')(null) >> {
                false
            }
            edgeXGeneric.getBinding().setVariable('env', [SILO: 'sandbox'])

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
            edgeXGeneric.getScripts(config, 'build_script', 'master') == expectedResult

        where:
            config << [
                [
                    branches: [
                        master: [
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
            edgeXGeneric.allScripts(config, 'pre_build_script', 'master') == expectedResult

        where:
            config << [
                [
                    branches: [
                        master: [
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
            edgeXGeneric.anyScript(config, 'pre_build_script', 'master') == expectedResult

        where:
            config << [
                [
                    branches: [
                        master: [
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
            edgeXGeneric.getBinding().setVariable('env', [EXTRA_SETTINGS: 'file1:var1,file2:var2,file3:var3'])
            getPipelineMock('configFile.call')(['fileId':'file1', 'variable':'var1']) >> 'result1'
            getPipelineMock('configFile.call')(['fileId':'file2', 'variable':'var2']) >> 'result2'
            getPipelineMock('configFile.call')(['fileId':'file3', 'variable':'var3']) >> 'result3'
        expect:
            edgeXGeneric.getConfigFilesFromEnv() == ['result1', 'result2', 'result3']
    }

    def "Test setupPath [Should] return expected [When] called - DD" () {
        setup:
            edgeXGeneric.getBinding().setVariable('PATH', '/usr/local/bin:/usr/bin:/bin')

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
