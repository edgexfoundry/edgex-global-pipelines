def call(branchName = env.GIT_BRANCH, body) {
    // what defines a main release branch
    def releaseStreams = [/.*master/, /.*california/, /.*delhi/, /.*edinburgh/, /.*git-semver/]

    if(branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true)) {
        println "[edgeXReleaseStage] Current branch IS valid release branch. Running code..."
        body()
    } else {
        println "[edgeXReleaseStage] Current branch IS NOT a valid release branch, skipping code..."
    }
}