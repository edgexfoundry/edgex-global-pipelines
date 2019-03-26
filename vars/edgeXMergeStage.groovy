def call(branchName = env.GIT_BRANCH, body) {
    // what defines a main release branch
    def releaseStreams = [/.*master/, /.*california/, /.*delhi/, /.*edinburgh/] //, /.*git-semver/

    if(branchName && (releaseStreams.collect { branchName =~ it ? true : false }).contains(true)) {
        println "[edgeXMergeStage] Current branch [${branchName}] IS valid release branch. Running code..."
        body()
    } else {
        println "[edgeXMergeStage] Current branch [${branchName}] IS NOT a valid release branch, skipping code..."
    }
}