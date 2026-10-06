# edgex-global-pipelines
[![Build Status](https://jenkins.edgexfoundry.org/view/EdgeX%20Foundry%20Project/job/edgexfoundry/job/edgex-global-pipelines/job/main/badge/icon)](https://jenkins.edgexfoundry.org/view/EdgeX%20Foundry%20Project/job/edgexfoundry/job/edgex-global-pipelines/job/main/) [![GitHub Latest Dev Tag)](https://img.shields.io/github/v/tag/edgexfoundry/edgex-global-pipelines?include_prereleases&sort=semver&label=latest-dev)](https://github.com/edgexfoundry/edgex-global-pipelines/tags) ![GitHub Latest Stable Tag)](https://img.shields.io/github/v/tag/edgexfoundry/edgex-global-pipelines?sort=semver&label=latest-stable) [![GitHub License](https://img.shields.io/github/license/edgexfoundry/edgex-global-pipelines)](https://choosealicense.com/licenses/apache-2.0/) [![GitHub Pull Requests](https://img.shields.io/github/issues-pr-raw/edgexfoundry/edgex-global-pipelines)](https://github.com/edgexfoundry/edgex-global-pipelines/pulls) [![GitHub Contributors](https://img.shields.io/github/contributors/edgexfoundry/edgex-global-pipelines)](https://github.com/edgexfoundry/edgex-global-pipelines/contributors) [![GitHub Committers](https://img.shields.io/badge/team-committers-green)](https://github.com/orgs/edgexfoundry/teams/devops-core-team/members) [![GitHub Commit Activity](https://img.shields.io/github/commit-activity/m/edgexfoundry/edgex-global-pipelines)](https://github.com/edgexfoundry/edgex-global-pipelines/commits)

## About

This repository contains useful Jenkins global library functions used within the EdgeX Jenkins build pipeline here: [https://jenkins.edgexfoundry.org](https://jenkins.edgexfoundry.org). You can learm more about Jenkins global libraries here: [https://jenkins.io/doc/book/pipeline/shared-libraries/](https://jenkins.io/doc/book/pipeline/shared-libraries/)

## Documentation

For more detailed documentation and tutorials visit the [EdgeX Global Pipelines Documentation Page](https://edgexfoundry.github.io/edgex-global-pipelines/html/)

## How to use

You can include this library by configuring your Jenkins instance on the <jenkins-url>/configure screen. Or you can load the library dynamically by using this code:

```Groovy
library(identifier: 'edgex-global-pipelines@main',
    retriever: legacySCM([
        $class: 'GitSCM',
        userRemoteConfigs: [[url: 'https://github.com/edgexfoundry-holding/edgex-global-pipelines.git']],
        branches: [[name: '*/main']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[
            $class: 'SubmoduleOption',
            recursiveSubmodules: true,
        ]]]
    )
) _
```

## GitHub Actions

### Dev Tag

[.github/workflows/dev-tag.yml](.github/workflows/dev-tag.yml) is a reusable workflow that creates the dev tag for a repository and bumps its semver. It replaces the `Semver` stage (`edgeXSemver tag/bump/push`) of the Jenkins pipelines.

Add a caller workflow to the repository, e.g. `.github/workflows/dev-tag.yml`:

```yaml
name: Dev Tag
on:
  push:
    branches: [main]
jobs:
  dev-tag:
    if: github.repository_owner == 'edgexfoundry'
    uses: edgexfoundry/edgex-global-pipelines/.github/workflows/dev-tag.yml@stable
    permissions:
      contents: write
```

On every push to the branch, the workflow:

1. Reads the current version from the `semver` branch, in the file named after the pushed branch (e.g. `semver:main`).
2. Creates the annotated tag `v<version>` on the pushed commit.
3. Bumps the version the same way as `git semver bump pre --prefix=dev`:
    - `X.Y.Z-dev.N` -> `X.Y.Z-dev.N+1`
    - `X.Y.Z` -> `X.Y.Z+1-dev.1`
4. Pushes the tag and the `semver` branch in one atomic push, so they never get out of sync.

Notes:

- The `semver` branch must already contain a version file for the branch. The workflow does not initialize it.
- If the commit is already tagged with a version, the workflow skips, so re-running a job is safe.
- Runs of the same repository are serialized. If several pushes queue up, only the newest pending run is kept, so a skipped commit gets no dev tag but versions stay in order.
- Tags created by this workflow are not signed. The Jenkins pipelines sign tags with Sigul (`edgeXInfraLFToolsSign`).