# Unit Testing Prescriptive Guidance

The edgex-global-pipelines shared library leverages the [Jenkins Spock framework](https://github.com/ExpediaGroup/jenkins-spock) for unit testing Jenkins pipeline scripts and functions. The Jenkins Spock unit test framework does not currently support unit testing of [Jenkins Declarative Pipeline code](https://www.jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline).

## Encapsulate Pipeline logic witin Groovy functions
In order to facilitate unit testing of the edgex-global-pipelines shared library, the DevOps team has made a deliberate effort to to minimize the amount of scripting logic contained within Jenkins declarative pipelines. This is accomplished by encapsulating pipeline logic within a Groovy function and calling the function in the declarative pipeline step as needed. Localizing pipeline logic within Groovy functions enables the Jenkins Spock framework to provide greater test coverage of Pipeline logic.

### Example
An example this approach can be seen within the `Build -> amd64 -> Prep` stage of the [edgeXBuildCApp Delcarative Pipeline](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/vars/edgeXBuildCApp.groovy). Note the logic for prepping the base build image is encapsulated into a method named `prepBaseBuildImage` and it is called within the declarative Pipeline. Also the `prepBaseBuildImage` function logic is thoroughly unit tested in the [edgeXBuildCApp Spec](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/src/test/groovy/edgeXBuildCAppSpec.groovy)