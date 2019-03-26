def call(vars) {
    if(vars != null) {
        vars.each { k, v ->
            env.setProperty(k, v)
            if(k == 'GIT_BRANCH') {
                env.setProperty('SEMVER_BRANCH', v.replaceAll( /^origin\//, '' ))
                env.setProperty('GIT_BRANCH_CLEAN', v.replaceAll('/', '_'))
            } else if(k == 'GIT_COMMIT') {
                env.setProperty('SHORT_GIT_COMMIT', env.GIT_COMMIT.substring(0,7))
            }
        }
    }

    // attempty to set a default architecture
    if(!env.ARCH) {
        def vmArch = sh(script: 'uname -m', returnStdout: true).trim()
        env.setProperty('ARCH', vmArch)
    }
}