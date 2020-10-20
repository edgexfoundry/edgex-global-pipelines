# edgex-global-pipelines

![build-status](https://jenkins.edgexfoundry.org/job/edgexfoundry/job/edgex-global-pipelines/job/master/badge/icon)

## About

This repository contains useful Jenkins global library functions used within the EdgeX Jenkins build pipeline here: [https://jenkins.edgexfoundry.org](https://jenkins.edgexfoundry.org). You can learm more about Jenkins global libraries here: [https://jenkins.io/doc/book/pipeline/shared-libraries/](https://jenkins.io/doc/book/pipeline/shared-libraries/)

## Documentation

For more detailed documentation and tutorials visit the [EdgeX Global Pipelines Documentation Page](https://edgexfoundry.github.io/edgex-global-pipelines/html/)

## How to use

You can include this library by configuring your Jenkins instance on the <jenkins-url>/configure screen. Or you can load the library dynamically by using this code:

```Groovy
library(identifier: 'edgex-global-pipelines@master', 
    retriever: legacySCM([
        $class: 'GitSCM',
        userRemoteConfigs: [[url: 'https://github.com/edgexfoundry-holding/edgex-global-pipelines.git']],
        branches: [[name: '*/master']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[
            $class: 'SubmoduleOption',
            recursiveSubmodules: true,
        ]]]
    )
) _
```