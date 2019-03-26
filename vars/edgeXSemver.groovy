def call(command = null, credentials = 'edgex-jenkins-ssh', debug = true) {
    def arch          = env.ARCH ?: 'x86_64'
    def gitSemverVersion = '0.0.1-pre.5'

    def semverImage   = "ernestoojeda/git-semver:${gitSemverVersion}-${arch}"
    def envVars       = [ 'SSH_KNOWN_HOSTS=/etc/ssh/ssh_known_hosts' ]
    def semverCommand = [
       'git',
       'semver'
    ]

    def semverVersion

    if(!command) {
        docker.image(semverImage).inside {
            semverVersion = sh(script: 'git semver', returnStdout: true).trim()
        }
    }
    else {
        if(debug) { envVars << 'SEMVER_DEBUG=on' }
        if(command) { semverCommand << command }

        docker.image(semverImage).inside('-v /etc/ssh:/etc/ssh') {
            withEnv(envVars) {
                sshagent (credentials: [credentials]) {
                    sh semverCommand.join(' ')
                }
            }

            semverVersion = sh(script: 'git semver', returnStdout: true).trim()
        }
    }
    semverVersion
}