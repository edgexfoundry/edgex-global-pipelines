
# edgeXBuildDocker

Shared Library to build docker images

## Parameters

* **project** - Specify your project name
* **dockerImageName** - Specify your Docker Image name
* **semver** - Set this `true` for Semantic versioning
* **dockerNexusRepo** - Specify Docker Nexus repository

## Usage

### Basic example

```groovy
edgeXBuildDocker (
    project: 'docker-edgex-consul',
    dockerImageName: 'docker-edgex-consul',
    semver: true
)
```

### Complex example

```groovy
edgeXBuildDocker (
    project: 'edgex-taf-common',
    mavenSettings: 'taf-settings',
    dockerNexusRepo: 'snapshots',
    semver: true
)
```
