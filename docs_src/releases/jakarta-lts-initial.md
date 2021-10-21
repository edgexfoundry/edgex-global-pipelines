# Jakarta LTS Changes

## cd-management

* New `release` branch in cd-management to manage lts. Maybe call it `lts`
* New field in YAML signifying that this is an LTS release:

```yaml
---
name: 'edgex-go'
version: '2.1.0'
releaseName: 'jakarta'
releaseStream: 'main'
lts: true # <-- new field
...
```

### Questions?

* Should we name the lts branch for all repos `lts`? OR use it's codename like `jakarta`?

This would trigger a new process in edgex-global-pipelines@edgeXRelease. See changes to edgex-global-pipelines

## ci-build-images

* Branch the `golang-1.16` branch to `golang-1.16-lts` or similar and push the resulting image to the Nexus release repo.
* Branch the `gcc` branch to `gcc-lts` or similar and push the resulting image to the Nexus release repo.

These images should be pretty simple as to push to Nexus release because edgeXBuildDocker allows us to specify the Nexus release repository as a target. See `dockerNexusRepo` in the [docs](https://edgexfoundry.github.io/edgex-global-pipelines/html/libraries/edgeXBuildDocker/).

## edgex-compose

3rd party docker images should be archive to the Nexus release repository. Images that should be archived are:

consul
kong
lfedge/ekuiper
postgres
redis
vault

A modification to the [Jenkinsfile](https://github.com/edgexfoundry/edgex-compose/blob/main/Jenkinsfile) can be make to introduce a new pipeline parameter to trigger a stage that gathers the existing 3rd party images, retags and pushes them to the Nexus release repository.

Sample script to extract and retag:

```bash
for image in $(grep "image:" ./docker-compose.yml | grep -v nexus | awk '{print $2}' | sort); do echo docker tag $image nexus3.edgexfoundry.org:10002/3rdparty/$image; echo docker push nexus3.edgexfoundry.org:10002/3rdparty/$image; done
```

## edgex-global-pipelines@edgeXRelease

Various changes will be needed in vars/edgexRelease.groovy to support the lts release. See <https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgeXRelease.groovy#L38>. If we are doing an LTS release we should not create a tag, we need to create a long running branch where changes can be made without affecting the main branch.

### Changes For Go Repositories

As called out in the [LTS release process](https://wiki.edgexfoundry.org/pages/viewpage.action?pageId=69173332#LongTermSupport(v2)-GoBasedProjects), for Go based projects, we will switch to Go vendoring for dependencies. This will give us 100% confidence that the dependencies will always be available in the LTS release and allow for easy patching and rebuilding.

* If the release is an LTS release and the **LTS branch does not exist**, we will need to run something similar to the following:

```bash
git checkout -b <lts-release-name>
grep -v vendor .gitignore > .gitignore.tmp
mv .gitignore.tmp .gitignore
make vendor
git add .
git commit -m "ci(lts-release): LTS release v<VERSION> @<commitId from release yaml>"
git push origin <lts-release-name>
```

* If the release is an LTS release and the **LTS branch does exist**, we will need to run something similar to the following:

```bash
git checkout <lts-release-name>
git commit --allow-empty -m "ci(lts-release): LTS release v<VERSION> @<commitId from release yaml>"
git push origin <lts-release-name>
```

### Changes For C Repositories

There is no dependency management for C based projects, so no dependency management changes are needed. We will need to branch and push though.

* If the release is an LTS release and the **LTS branch does not exist**, we will need to run something similar to the following:

```bash
git checkout -b <lts-release-name>
git commit --allow-empty -m "ci(lts-release): LTS release v<VERSION> @<commitId from release yaml>"
git push origin <lts-release-name>
```

* If the release is an LTS release and the **LTS branch does exist**, we will need to run something similar to the following:

```bash
git checkout <lts-release-name>
git commit --allow-empty -m "ci(lts-release): LTS release v<VERSION> @<commitId from release yaml>"
git push origin <lts-release-name>
```

## edgex-global-pipelines General Changes

### Semver notes and noop builds

* Git semver will continue to be used for lts releases. After the initial release a pre-release dev tag will be created as usual. However, we have to introduce the concept of a noop (no operation) build due to the fact we are branching and tagging. We can use the commit message as a way to determine a noop build. The flow will go something like this:

    1. 1st build, triggered by push of lts branch to GitHub: No op, no semver needed
    2. edgeXRelease creates force creates tag 2.1.0
    3. 2nd build, triggered by edgeXRelease and builds the 2.1.0 code and pushes the release to Nexus
    4. Bump semver to 2.1.1-dev.1

* **vars/edgex.groovy**
  * Potentially need to add a new method similar to `isReleaseStream()` called `isLTS()`.
  * Add method or modify [getGoLangBaseImage()](https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgex.groovy#L207) to return the proper released ci-build-image if this is an LTS release
* **vars/edgeXBuildGoApp.groovy**
  * For LTS releases we need to use the released CI base build image referenced in the ci-build-images section. So we will need to modify this method [prepBaseBuildImage()](https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgeXBuildGoApp.groovy#L560) to return the proper base build image if the release is an LTS release. Will call `edgex` function above.
* **vars/edgeXBuildGoParallel.groovy**
  * For LTS releases we need to use the released CI base build image referenced in the ci-build-images section. So we will need to modify this method [prepBaseBuildImage()](https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgeXBuildGoParallel.groovy#L481) to return the proper base build image if the release is an LTS release. Will call `edgex` function above.
* **vars/edgeXBuildCApp.groovy**
  * This will be the most complicated change. We will need to release/archive the build images created from the Dockerfile.build. These images will need to be pushed to nexus release and then used as the base images for all subsequent LTS builds. This will ensure we have 100% reproducibility of the C repositories. For example we will need to create a `device-coap-c` specific docker image will all build dependencies archived in the image.
  * A new stage will need to be added to the pipeline to release the docker build images. This stage can either be triggered by a new parameter to the pipeline, or can potentially be triggered by a special commit message. Since we are already doing a special commit message for the initial push, the commit message may be the correct approach.
  * Changes will be required to the [prepBaseBuildImage()](https://github.com/edgexfoundry/edgex-global-pipelines/blob/main/vars/edgeXBuildCApp.groovy#L444) function, to use repo level build images if we are on an LTS branch.


