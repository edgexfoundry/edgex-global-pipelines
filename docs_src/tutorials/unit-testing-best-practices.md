# Unit Testing Best Practices

# Table Of Contents
- [Unit Testing Declarative Pipelines](#unit-testing-declarative-pipelines)
  * [Encapsulate Pipeline logic within Groovy functions](#encapsulate-pipeline-logic-within-groovy-functions)
  * [Example](#example)
- [Mocking Jenkins Dependencies](#mocking-jenkins-dependencies)
  * [Add plugin dependency to Gradle](#add-plugin-dependency-to-gradle)
- [Mocking Environment Variables](#mocking-environment-variables)
  * [Testing environment variables](#testing-environment-variables)
- [Mock external shared library methods](#mock-external-shared-library-methods)
  * [Integration Testing](#integration-testing)
- [Mock errors](#mock-errors)
- [Mock external shared library methods](#mock-external-shared-library-methods-1)
  * [Call Graph Example](#call-graph-example)
- [References](#references)

# Unit Testing Declarative Pipelines

The edgex-global-pipelines shared library leverages the [Jenkins Spock framework](https://github.com/ExpediaGroup/jenkins-spock) for unit testing Jenkins pipeline scripts and functions. The Jenkins Spock unit test framework does not currently support unit testing of [Jenkins Declarative Pipeline code](https://www.jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline).

## Encapsulate Pipeline logic within Groovy functions

In order to facilitate unit testing of the edgex-global-pipelines shared library, the DevOps team has made a deliberate effort to to minimize the amount of scripting logic contained within Jenkins declarative pipelines. This is accomplished by encapsulating pipeline logic within a Groovy function and calling the function in the declarative pipeline step as needed. Localizing pipeline logic within Groovy functions enables the Jenkins Spock framework to provide greater test coverage of Pipeline logic.

## Example
An example this approach can be seen within the `Build -> amd64 -> Prep` stage of the [edgeXBuildCApp Delcarative Pipeline](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/vars/edgeXBuildCApp.groovy). Note the logic for prepping the base build image is encapsulated into a method named `prepBaseBuildImage` and it is called within the declarative Pipeline. Also the `prepBaseBuildImage` function logic is thoroughly unit tested in the [edgeXBuildCApp Spec](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/src/test/groovy/edgeXBuildCAppSpec.groovy)


# Mocking Jenkins Dependencies
Always leverage the builtin capabilities of the Jenkins-Spock framework for mocking Jenkins plugins. For example, if you come across the following error when unit testing your code:
```
java.lang.IllegalStateException: During a test, the pipeline step [stepName] was called but there was no mock for it.
```
The error above denotes that the code under test calls a pipeline step `stepName` but there is no mock for it. You are able to explicitly mock the pipeline step using `explictlyMockPipelineStep` method available in the Jenkins-Spock framework. However it is recommended that the plugin that contains the corresponding step be added as a dependency in the `build.gradle` file. For instructions on how to do this, refer to the [Add plugin dependency to Gradle](#add-plugin-dependency-to-gradle) section.

## Add plugin dependency to Gradle
1. Note the name of the Pipeline Step to add.
2. Go to [Pipeline Steps Reference](https://www.jenkins.io/doc/pipeline/steps/) page.
3. Use your browser and search for the Pipeline Step within the page.
4. If the Pipeline Step is found, click on the Pipeline that it belongs to, the page for the respective Pipeline should open.
5. Under the heading click on the `View this plugin on the Plugins site` link, the plugins.jenkins.io page should open.
6. In the plugins.jenkins.io page note the ID for the Pipeline. You will use this ID in the next step.
7. Go to [Maven Repository](https://mvnrepository.com/) page.
8. Enter the ID in the search, and locate the result from the results displayed, click on the respective link.
9. In the page, click on the `Jenkins Releases` tab.
10. If you know the version then click it, otherwise click on the latest version that is listed.
11. In the Gradle tab, note the group, name and version.
12. Edit the `build.gradle` file, add the dependency found above to the dependencies section.

# Mocking Environment Variables
Always ensure the source code under test uses one of the following idioms for getting or setting Environment Variables, doing this will simplify the ability to mock environment variables in the unit test:
- Getting the value of an environment variable
  - `env.VARIABLE`
  - `env[VARIABLE]`
  - `"${env.VARIABLE}"`
- Setting the value of an environment variable
  - `env.VARIABLE = VALUE`
  - `env[VARIABLE] = VALUE`

## Testing environment variables
Within your unit tests, environment variables are set using the `.getBinding().setVariable('name', 'value')` idiom. Where the name is `env` and the value is a map you define within your unit test. The map should define all environment variables the code under test expects, likewise the map can be used to assert any environment variables that the code under test sets.

A good example of this practice is the [EdgeXSetupEnvironmentSpec](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/src/test/groovy/edgeXSetupEnvironmentSpec.groovy)

# Mock external shared library methods
The `edgex-global-pipelines` Jenkins shared library consists of multiple scripts exposing methods for various functional areas, where each script is named after the particular functional area it serves. The shared library includes a `EdgeX` script that serves as utility script containing methods that are shared amongst other scripts. It is common practice for a method in one script call a method in another script, to mock the interaction you use the `explictlyMockPipelineVariable` to mock the script, then `getPipelineMock` method to verify the interaction or stub it if necessary. 

Mock the external script named `script`:
```
explictlyMockPipelineVariable('script')
```
It is recommended to mock all external scripts called within the script under test in the Test Spec setup.

Get the script mock and stub the call to `method` to return `'value'` for any argument passed in:
```
getPipelineMock('script.method').call(_) >> 'value'
```

## Integration Testing
Integration Testing is defined as a type of testing where software modules are integrated logically and tested as a group. The Jenkins-Spock framework provides the ability to load any number of scripts to test within a given Spec Test. There are instances where performing integration tests is more practical, if you wish to do so then we recommend naming the Spec Test with `Int` as to differentiate between unit and integration tests.

A good example of this practice is the [EdgeXReleaseDockerImageIntSpec](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/src/test/groovy/edgeXReleaseDockerImageIntSpec.groovy)

# Mock errors
Always leverage `error` when wanting to conditionally abort part of your script. Error is a Pipeline Step whose plugin has been added as a dependency to our project thus is already mocked by the framework. An example showing how you can assert that an error is thrown with a specific message:
```
1 * getPipelineMock('error').call('error message')
```

# Mock external shared library methods
The difficulties of mocking functions within the same script under test have been described in the following issue: [Issue 78](https://github.com/ExpediaGroup/jenkins-spock/issues/78). Due to the nature of how the scripts that comprise the `edgex-global-pipelines` shared library are written; where a deliberate intent is made to develop small, functionally cohesive methods that contribute to a single well-defined task. This development intent results in having scripts with multi-layered call graphs, where methods may call multiple methods from within the same script. We find that the workaround provided in the issue is complicated and doesn't scale well in our environment. For these reasons the method outlined below is being suggested.

1. For the script under test, document its call graph. A call graph is a control flow graph, which represents calling relationships between methods in a script or program. Each node represents a method and each edge (f, g) indicates that method f calls method g. An example [EdgeXReleaseGitTag call graph](#call-graph-example) is depicted below.
2. Create a second script with the same name as the original script with the word Util added to the end, for example `EdgeXReleaseGitTagUtil.groovy`.
3. Analyze the call graph, methods that reside in odd numbered layers should continue to reside in the first script, methods at even numbered layers should be moved from the first script into the second script.
4. Create a Spec Test for both scripts.

Mocking of methods between both scripts follow the same pattern described for [Mock external shared library methods](#mock-external-shared-library-methods). The only difference with this approach is that the scripts are (for the lack of a better word) name spaced for the respective functional area.

## Call Graph Example
<img src="https://user-images.githubusercontent.com/3664215/88595450-cd92da00-d017-11ea-8838-d2c2f50b831a.png" width=70% height=70%>

**NOTE** The approach outlined above is not recommended as the standard development approach, but as an alternative to re-writing the script under test if mocking of the internal method calls becomes unwieldy.

# References
- [Jenkins Spock Documentation](https://www.javadoc.io/doc/com.homeaway.devtools.jenkins/jenkins-spock/latest/com/homeaway/devtools/jenkins/testing/JenkinsPipelineSpecification.html)
- [Spock Framework Reference](http://spockframework.org/spock/docs/1.3/all_in_one.html)
- [Jenkins Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)

