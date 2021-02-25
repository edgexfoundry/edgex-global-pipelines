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

/*
    Usage: 
*/

def call(config = [:]) {
    def renderedEmailTemplate = edgeXEmailUtil.generateEmailTemplate()

    // println "We have email HTML to send?" //debug
    // println renderedEmailTemplate //debug

    def buildStatus = currentBuild.result == null ? "SUCCESS" : currentBuild.result
    def subject     = config.subject ?: "[${buildStatus}] ${env.JOB_NAME} Build #${env.BUILD_NUMBER}"
    def recipients  = config.emailTo

    println "[edgeXEmailHelper] config: ${config}"

    if(renderedEmailTemplate && recipients) {
        mail body: renderedEmailTemplate, subject: subject, to: recipients, mimeType: 'text/html'
    } else {
        println "[edgeXEmailHelper] No email message could be generated. Not sending email"
    }
}
