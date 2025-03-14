def call(config) {
    edgex.bannerMessage "[edgeXBuildMultiArch] RAW Config: ${config}"
    pipeline {
        agent {
            label 'centos7-docker-4c-2g'
        }
        options {
            timestamps()
            quietPeriod(5) // wait a few seconds before starting to aggregate builds...??
            durabilityHint 'PERFORMANCE_OPTIMIZED'
            timeout(360)
        }
        triggers {
            issueCommentTrigger('.*^recheck$.*')
        }
        stages {
            stage('Create Multi-Arch Images') {
                steps {
                    edgeXDockerLogin(settingsFile: 'ci-build-images-settings')
                    bootstrapBuildX()

                    script {
                        //def images = sh(script: "grep image docker-compose.yml | grep -v edgexfoundry | awk '{print \$2}'", returnStdout: true).trim()
                        def images = ["nexus3.edgexfoundry.org:10004/sample-service:latest"]
                        images.each { image ->
                            sh "echo -e 'FROM ${image}' | docker buildx build --platform 'linux/amd64,linux/arm64' -t ${image} --push -"
                        }
                    }
                }
            }
        }
        post {
            always {
                edgeXInfraPublish()
            }
            cleanup {
                cleanWs()
            }
        }
    }
}

def bootstrapBuildX() {
    sh 'docker buildx ls'
    sh 'docker buildx create --name edgex-builder --platform linux/amd64,linux/arm64 --use'
    sh 'docker buildx inspect --bootstrap'
    sh 'docker buildx ls'
}