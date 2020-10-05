

#edgeXBuildGoApp

Shared Library to build Go projects

##Parameters
* **project** - Specify your project name
* **goVersion** - Go version

##Example Usage

```bash
edgeXBuildGoApp (
project: 'device-random-go',
goVersion: '1.13'
)
```
```bash
edgeXBuildGoApp (
project: 'app-functions-sdk-go',
semver: true,
goVersion: '1.13',
testScript: 'make test',
buildImage: false,
publishSwaggerDocs: true,
swaggerApiFolders: ['openapi/v2']
)
```
