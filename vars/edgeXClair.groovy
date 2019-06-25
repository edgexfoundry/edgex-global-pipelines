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

/*
   Usage: edgeXClair('dockerImage:tag')
   JSON Output: edgeXClair('dockerImage:tag', [outputFormat: 'json'])
*/
def call(image, Map options = [:]) {
    def klarImage = options.klarImage ?: 'nexus3.edgexfoundry.org:10004/edgex-klar:latest'
    def server    = options.server    ?: 'edgex.clair.adsdcsp.com'
    
    if(!image) {
        throw new Exception("edgeXClair scanner requires docker image to scan: [edgeXClair('dockerImage:tag')]")
    }

    // TODO: need to install pipeline utility plugin to convert json to map
    def clairJson = scan(image, server, klarImage, 'json')

    if(clairJson) {
        // scan again with to get results in table format for HTML
        def table = scan(image, server, klarImage, 'table')

        def reportDir = 'clair-reports'
        
        sh "mkdir -p ${WORKSPACE}/${reportDir}"
        
        def filename = "clair_results_${getImageName(image).replaceAll(':', '_')}.html"
        def html = tableHtml(image, table.replaceAll("\u001B\\[[;\\d]*m", ""))
        println "Generated HTML Table Report. Writing to ${WORKSPACE}/${reportDir}/${filename}"
        
        writeFile(file: "./${reportDir}/${filename}", text: html)
    
        archiveArtifacts allowEmptyArchive: true, artifacts: "${reportDir}/${filename}"
    }

    clairJson
}

def scan(image, server, klarImage, outputFormat) {
    def output
    docker.image(klarImage).inside('--entrypoint=') {
        withEnv(["CLAIR_ADDR=${server}", "FORMAT_OUTPUT=${outputFormat}"]) {
            // piping the output to tee so that exitcode is always 0 for now
            output = sh(script: "/klar ${image} | tee", returnStdout: true).trim()
            
            // parse json.
            if(outputFormat == 'json') {
                if(output) {
                    try {
                        output = readJSON(text: output)
                    } catch(Exception ex) {
                        println "[edgexClair] Unable to parse JSON"
                        println output
                        // could not parse json. Just assume no results.
                        output = [ LayerCount: 0, Vulnerabilities: [:]]
                    }
                } else {
                    output = [ LayerCount: 0, Vulnerabilities: [:]]
                }
            }
        }
    }
    output
}

def getImageName(image) {
    def imageSplit = image.split('/')
    if(imageSplit.length > 1) {
        imageSplit[imageSplit.length-1]
    } else {
        image
    }
}

def tableHtml(image, tableContent) {
    return """
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
    integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
  <style>
    body { padding: 10px; }
  </style>
  <title>Clair Scan Results for Image [${image}]</title>
</head>
<body>
    <h1>Scan Results for Docker Image [${image}]</h1>
    <pre>
${tableContent}
    </pre>
</body>
</html>
"""
}
