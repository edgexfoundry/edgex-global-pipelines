
#edgeXBuildGoApp

Shared Library to build Go projects

## Parameters

* **project** - Specify your project name
* **goVersion** - Go version

## Usage

### Basic example

```groovy
edgeXBuildGoApp (
    project: 'device-random-go',
    goVersion: '1.15'
)
```

### Complex example

```groovy
edgeXBuildGoApp (
    project: 'app-functions-sdk-go',
    semver: true,
    goVersion: '1.15',
    testScript: 'make test',
    buildImage: false,
    publishSwaggerDocs: true,
    swaggerApiFolders: ['openapi/v2']
)
```
