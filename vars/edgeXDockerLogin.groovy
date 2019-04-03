def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    
    if(body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }

    // The LF Global JJB Docker Login script looks for information in the following variables: 
    // $SETTINGS_FILE, $DOCKER_REGISTRY, $REGISTRY_PORTS, $DOCKERHUB_REGISTRY, $DOCKERHUB_EMAIL
    // Please refer to the shell script in global-jjb/shell for the usage.
    // Most parameters are listed as optional, but without any of them set the script has no operation.
    def _dockerRegistry = config.dockerRegistry ?: ''
    def _dockerRegistryPorts = config.dockerRegistryPorts ?: ''
    def _dockerHubRegistry = config.dockerHubRegistry ?: ''
    def _dockerHubEmail = config.dockerHubEmail ?: ''

    def _settingsFile = config.settingsFile
    if(!_settingsFile) {
        throw new Exception('Project Settings File id (settingsFile) is required for the docker login script.')
    }

    if(_dockerRegistry && !_dockerRegistryPorts) {
        throw new Exception('Docker registry ports (dockerRegistryPorts) are required when docker registry is set (dockerRegistry).')
    }

    if(_dockerRegistryPorts && !_dockerRegistry) {
        throw new Exception('Docker registry (dockerRegistry) is required when docker registry ports are set (dockerRegistryPorts).')
    }

    def overrideVars = []
    if(_dockerRegistry) { overrideVars << "DOCKER_REGISTRY=${_dockerRegistry}" }
    if(_dockerRegistryPorts) { overrideVars << "REGISTRY_PORTS=${_dockerRegistryPorts}" }
    if(_dockerHubRegistry) { overrideVars << "DOCKERHUB_REGISTRY=${_dockerHubRegistry}" }
    if(_dockerHubEmail) {overrideVars << "DOCKERHUB_EMAIL=${_dockerHubEmail}" }

    withEnv(overrideVars){
        configFileProvider([configFile(fileId: _settingsFile, variable: 'SETTINGS_FILE')]) {
        sh(script: libraryResource('global-jjb-shell/docker-login.sh'))
        }
    }
}