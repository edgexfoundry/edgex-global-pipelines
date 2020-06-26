//
// Copyright (c) 2020 Intel Corporation
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

edgeXSwaggerPublish(releaseYaml)
/*
   Usage: edgeXSwaggerPublish() # defaults to EdgeXFoundry1
   Usage: edgeXSwaggerPublish('custom-owner')
*/

def call(owner = 'EdgeXFoundry1') {
    try{
        println("[edgeXSwaggerPublish]: Retrieving edgex-global-pipeline resource shell scripts. - DRY_RUN: ${env.DRY_RUN}")
        retreieveResourceScripts(["toSwaggerHub.sh", "edgex-publish-swagger.sh"])
        def dryRun = edgex.isDryRun()
        withEnv(["SWAGGER_DRY_RUN=${dryRun}"]) {
            sh "./edgex-publish-swagger.sh ${owner}"
        }
    }
    catch(Exception ex) {
        error("[edgeXSwaggerPublish]: ERROR occurred publishing to SwaggerHub: ${ex}")
    }
}

def retreieveResourceScripts(files){ 
    files.each{ file ->
        print("[edgeXSwaggerPublish] Retrieving: ${file}")
        def textDestination = libraryResource(file)
        writeFile(file: "./${file}", text: textDestination)
        sh "chmod +x ./${file}"
    }
}