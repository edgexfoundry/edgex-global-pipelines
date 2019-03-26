def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    
    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    def gitCheckoutExtensions = []

    if(config.tags) {
        gitCheckoutExtensions << 'tags'
    }

    if(config.lfs) {
        gitCheckoutExtensions << 'lfs'
    }

    def gitVars = null
    
    if(gitCheckoutExtensions) {
        def ex = []

        if(gitCheckoutExtensions.contains('tags')) {
            ex << [$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']
        }
        if(gitCheckoutExtensions.contains('lfs')) {
            ex << [$class: 'GitLFSPull']
        }

        gitVars = checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
            extensions: ex,
            userRemoteConfigs: scm.userRemoteConfigs,
        ])
    } else {
        gitVars = checkout scm
    }

    // setup git environment variables
    edgeXSetupEnvironment(gitVars)

    gitVars
}