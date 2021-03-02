//
// Copyright (c) 2021 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

def getJobDetailsJson() {
    def colorMap = [
        UNSTABLE: "#ffa84f",
        FAILURE: "#e60d49",
        SUCCESS: "#2dce84"
    ]

    // Due to some inaccuracies with Jenkins GIT env vars, need to grab details with git commands
    def commit, author, message, changeLog, branch

    try {
        commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
    } catch(ex) {}
    
    if(commit) {
        try {
            author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
        } catch(ex) {
            author = 'Unknown Author'
        }

        try {
            message = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
        } catch(ex) {
            message = 'Unknown Commit Message'
        }

        try {
            changeLogText = sh(returnStdout: true, script: "git log -m -1 --name-only --pretty='format:' ${commit}")
            changeLog = changeLogText.trim().split('\n')
        } catch(ex) {
            changeLog = []
        }

        try {
            branch = env.GIT_BRANCH ?: sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
        } catch(ex) {
            branch = 'Unknown Branch'
        }
    }

    def gitUrl
    if(env.GIT_BASE) {
        // Get a clean GIT url base
        def gitBase = env.GIT_BASE.substring(0, env.GIT_BASE.indexOf('edgexfoundry/$PROJECT'))
        gitUrl = "${env.GIT_URL.contains('git@') ? gitBase + env.GIT_URL.split(':')[1] : env.GIT_URL}"
    }

    def buildStatus = currentBuild.result == null ? "SUCCESS" : currentBuild.result // build result is null if the job is not completed
    def extractedBuildLog = sh(script: libraryResource('build-log-extract.sh'), returnStdout: true).trim()

    def extractedBuildLogSplit = extractedBuildLog.split('\n')
    def failedStage = extractedBuildLog ? extractedBuildLogSplit[0] : 'Could not determine failed stage'

    def buildLog

    if(extractedBuildLog) {
        buildLog = []
        for(def i = 2; i < extractedBuildLogSplit.size(); i++) { // drop the first two lines
            buildLog << extractedBuildLogSplit[i]
        }
        buildLog = buildLog.join('\n')
    } else {
        buildLog = 'An error occurred getting build log details...'
    }

    def buildColor  = colorMap[buildStatus]

    def jobDetails = [
        jobName: "${env.JOB_NAME} #${env.BUILD_NUMBER}",
        buildNumber: env.BUILD_NUMBER,
        buildUrl: env.BUILD_URL,
        gitUrl: gitUrl,
        buildConsoleUrl: "${env.BUILD_URL}console",
        color: buildColor,
        status: buildStatus,
        author: author,
        branch: branch,
        duration: currentBuild.durationString.replaceAll(' and counting', ''),
        commitMessage: message,
        failedStage: failedStage,
        changeLog: changeLog,
        buildLog: buildLog
    ]

    println "[edgeXEmailUtil] Job JSON"
    println jobDetails

    jobDetails
}

// my approach for this is to write the job details to a JSON file
// then use a templating tool to merge a tem
def generateEmailTemplate(jobDetails = null) {
    // allow jobDetails to be passed in for easier unit testing
    jobDetails = jobDetails == null ? getJobDetailsJson() : jobDetails

    def emailHTML
    if(jobDetails) {
        println "Writing Job Details to JSON"
        
        def jsonFile = "/tmp/jobdetails-${env.BUILD_NUMBER}.json"
        writeJSON file: jsonFile, json: jobDetails

        // need to write this the local workspace
        def emailTemplate = libraryResource('email/build-notification-template.html')
        writeFile file: '/tmp/job-email-template.html', text: emailTemplate

        docker.image('node:alpine').inside('-u 0:0 -v /tmp:/tmp') {
            sh 'npm install -g mustache' // slightly inneficient
            sh "mustache ${jsonFile} /tmp/job-email-template.html ${env.WORKSPACE}/email-rendered.html" // file needs to be written to workspace for readFile to work properly
        }

        emailHTML = readFile './email-rendered.html'
    }

    emailHTML
}