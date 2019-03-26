def call(branchName = env.GIT_BRANCH, body) {
    // what defines a main release branch
    def releaseStreams = [/.*master/, /.*california/, /.*delhi/, /.*edinburgh/, /.*git-semver/]

    if(branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true)) {
        println "[edgeXPRStage] Current branch [${branchName}] IS a valid release branch, skipping code..."
    } else {
        println "[edgeXPRStage] Not a release [${branchName}] branch, must be in a feature branch or PR"
        body()
    }
}