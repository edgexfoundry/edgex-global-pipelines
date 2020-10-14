
# edgeXBuildCApp

Shared Library to build C projects

## Parameters

* **project** - Specify your project name
* **dockerBuildFilePath** - Specify your Docker Build file Path
* **dockerFilePath** - Specify your Dockerfile path
* **pushImage** - Set this `false` if you dont want to push the image to DockerHub, by default `true`

## Usage

### Basic example

```groovy
edgeXBuildCApp (
    project: 'device-bacnet-c',
    dockerBuildFilePath: 'scripts/Dockerfile.alpine-3.9-base',
    dockerFilePath: 'scripts/Dockerfile.alpine-3.9'
)
```

### Complex example

```groovy
edgeXBuildCApp (
    project: 'device-sdk-c',
    dockerBuildFilePath: 'scripts/Dockerfile.alpine-3.11-base',
    dockerFilePath: 'scripts/Dockerfile.alpine-3.11',
    pushImage: false
)
```
