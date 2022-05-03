//
// Copyright (c) 2019 Intel Corporation
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

/**
 # edgeXSnyk

 ## Overview

 Shared library containing a useful set of functions to help with the creation of semantic versioning using the git-semver python library.
 The main call function builds the `git semver` command based on the provided input.

 **Please note:** this shared library is responsible for setting the `VERSION` environment variable during `git semver init` execution.

 ## Parameters

 Name | Required | Type | Description and Default Value
 -- | -- | -- | --
 command | false | str | Specify which Snyk command to run. Possible values: `test`, `monitor`. <br /><br />**Default:** `monitor` |
 dockerImage | false | string | If scanning a docker image either a local image name or remote image name. |
 dockerFile | false | string | If scanning a docker image, the path to `Dockerfile` relative to the Jenkins `WORKSPACE`. |
 severity | false | string | Severity threshold to mark the build as `unstable`. |
 sendEmail | false | string | Whether or not to send an email of the findings. <br /><br />**Default:** `true` |
 emailTo | false | string | Recipient list of who to send the email to. |
 htmlReport | false | string | Whether or not to generate an HTML report of findings. <br /><br />**Default:** `false` |
 
 ## Usage

 Test and continuously monitor project **dependencies**. For Go projects, this is typically the `go.mod` file:

 ```groovy
 edgeXSnyk()
 ```

 Test docker image for vulnerabilities and output results to Jenkins console:

 ```groovy
 edgeXSnyk(
    command: 'test',
    dockerImage: 'nexus3.edgexfoundry.org:10004/core-command:latest',
    dockerFile: '<path to Dockerfile>'
 )
 ```

 Test docker image for vulnerabilities and send email of findings:

 ```groovy
 edgeXSnyk(
    command: 'test',
    dockerImage: 'nexus3.edgexfoundry.org:10004/core-command:latest',
    dockerFile: '<path to Dockerfile>',
    severity: 'high',
    sendEmail: true,
    emailTo: <email address(s)>,
    htmlReport: true
 )
 ```
*/

def call(config = [:]) {
    def snykCmd           = config.command ?: 'monitor'
    def dockerImage       = config.dockerImage
    def dockerFile        = config.dockerFile
    def severityThreshold = config.severity // ?: 'high'
    def sendEmail         = edgex.defaultTrue(config.sendEmail)
    def snykRecipients    = config.emailTo
    def htmlReport        = edgex.defaultFalse(config.htmlReport)

    def snykScannerImage  = 'nexus3.edgexfoundry.org:10003/edgex-devops/edgex-snyk-go:1.410.4'
    def org               = env.SNYK_ORG ?: 'edgex-jenkins'

    def globalIgnorePolicy = config.ignorePolicy ?: 'https://raw.githubusercontent.com/edgexfoundry/security-pipeline-policies/main/snyk/.snyk'

    // Run snyk monitor by default
    def command = ['snyk', snykCmd, "--org=${org}"]

    println "[edgeXSnyk] dockerImage=${dockerImage}, dockerFile=${dockerFile}"

    // If docker specified alter test command
    if(dockerImage != null && dockerFile != null) {
        command << "--docker ${dockerImage}"
        command << "--file=./${dockerFile}"
    }

    if(snykCmd == 'test') {
        if(severityThreshold) {
            command << "--severity-threshold=${severityThreshold}"
        }

        try {
            println "[edgeXSnyk] downloading global ignore policy file from: ${globalIgnorePolicy}"
            sh "set -o pipefail ; curl -s '${globalIgnorePolicy}' | tee .snyk"
        }
        catch (ex) {
            // catching this only to print this message in the log will make it easier to know if ignore policy is being used
            println "[edgeXSnyk] Could not download the policy file. No findings will be ignored"
        }
        
        // force to look for .snyk file. If it doesn't exist, it just ignores this paramter
        command << '--policy-path=./.snyk'

        // TODO: make log/report name dynamic including image name
        if(htmlReport) {
            command << '--json | snyk-to-html -o snykReport.html'
        } else {
            command << '| tee snykResults.log'
        }
    }

    def exitCode = -1
    withCredentials([string(credentialsId: 'snyk-cli-token', variable: 'SNYK_TOKEN')]) {
        docker.image(snykScannerImage).inside("-u 0:0 --privileged -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=''") {
            def snykScript = "set -o pipefail ; ${command.join(' ')}"
            if(htmlReport) {
                sh 'rm -f snykReport.html'
            }
            println "[edgeXSnyk] command to run: ${snykScript}"
            exitCode = sh(script: snykScript, returnStatus: true)
        }
    }

    if(exitCode > 0) {
        println "[edgeXSnyk] Exit Code: ${exitCode}"
        // https://support.snyk.io/hc/en-us/articles/360003812578-CLI-reference

        // Exit codes only applicable to snyk test
        // Exit code 0 This means Snyk did not find vulnerabilities in your code an exited the process without failing the job.
        // Exit code 1 This means Snyk found vulnerabilities in your code and have failed the build
        // Exit code 2 This means Snyk exited with an error, please re-run with `-d` to see further information.
        // Exit code 3 This means Snyk did not detect any supported projects/manifests to scan. Re-check the command or if the command should run in a different directory.

        if(exitCode == 1) {
            println "[edgeXSnyk] Possible vulnerabilities have been found in the Snyk scan. Marking as build as unstable."

            if(sendEmail && snykRecipients) {
                // send email to committer and secuirty group here
                def subject = "[Snyk] Possible vulnerabilities discovered in ${env.PROJECT ? '[' + env.PROJECT + ']' : ''} scan"

                if(htmlReport) {
                    def messageBody = readFile(file: './snykReport.html')

                    mail body: messageBody, subject: subject, to: snykRecipients, mimeType: 'text/html'
                } else {
                    def messageBody = """Possible vulnerablities have been found by Snyk in for the following build: ${env.JOB_NAME}
More details can be found here:
===============================================
Build URL: ${env.BUILD_URL}
Build Console: ${env.BUILD_URL}console

Snyk Log:
${readFile(file: './snykResults.log')}
"""
                    mail body: messageBody, subject: subject, to: snykRecipients
                }
            }

            if(snykCmd == 'test') {
                if(htmlReport) {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'snykReport.html'
                } else {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'snykResults.log'
                }
            }

            unstable(message: 'Snyk Found Vulnerabilites')
        }
        // 2 and up
        else {
            error("[edgeXSnyk] An error occurred during the snyk scan see console output for details.")
        }
    }
}