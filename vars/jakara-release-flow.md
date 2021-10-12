# Jakarta Release Flows

## C Based Services

### Release Flow

- [New] edgeXRelease: creates and pushes "jakarta" branch at specific git sha
  - The push of the tag triggers new LTSRelease build
  - Job: <https://jenkins.edgexfoundry.org/blue/organizations/jenkins/edgexfoundry%2Fdevice-coap-c/detail/lts-test/17/pipeline/81>
  - No Op build, builds project specific relevant ci build images (x86_64, arm64) will all dependencies
  - Image is pushed to nexus release, i.e. nexus3.edgexfoundry.org:10002/device-coap-c-builder-{ARCH}:jakarta
- [Existing] edgeXRelease: tags git sha with release version e.g. 2.1.0
- [Existing] edgeXRelease: stages build artifact, i.e. triggers device-coap-c/jakarta job
  - [New] If this is a C build and LTS we will need to wait until the first LTSRelease build is done before running this build (not sure how to do this, maybe a timeout or manual input in the job)
  - Job: <https://jenkins.edgexfoundry.org/blue/organizations/jenkins/edgexfoundry%2Fdevice-coap-c/detail/lts-test/18/pipeline/154>
- [Existing] edgeXRelease: Bump Semver, i.e. next dev tag 2.1.1-dev.1

### PR Fixes (Regular Dev Process)

PR is open in fork as normal, but target branch will be LTS (jakarta) branch

- [Existing] User open's PR as normal for fix, etc <>
  - [New] Pipeline will detect the merge target is an LTS branch and will use released ci build image from the release process rather than build a ci image on the fly
  - `prepBaseBuildImage()` was the perfect abstraction to do this!

    ```bash
    docker pull nexus3.edgexfoundry.org:10002/device-coap-c-builder-x86_64:lts-test
    docker tag nexus3.edgexfoundry.org:10002/device-coap-c-builder-x86_64:lts-test ci-base-image-x86_64
    ```

---

## Go based services

### Release Flow

- [New] edgeXRelease: creates and pushes "jakarta" branch at specific git sha
  - The push of the tag triggers new LTSRelease build
  - Job: <https://jenkins.edgexfoundry.org/blue/organizations/jenkins/edgexfoundry%2Fsample-service/detail/lts-test/22/pipeline>
  - LTSRelease build will be a no op build in the case of Golang
- [Existing] edgeXRelease: tags git sha with release version e.g. 2.1.0
- [Existing] edgeXRelease: stages build artifact, i.e. triggers device-coap-c/jakarta job
  - [New] No wait needed as of yet, but maybe will need something in the future
  - Job: <https://jenkins.edgexfoundry.org/blue/organizations/jenkins/edgexfoundry%2Fsample-service/detail/lts-test/23/pipeline> (need to add lts branches to isReleaseStream)
- [Existing] edgeXRelease: Bump Semver, i.e. next dev tag 2.1.1-dev.1

### PR Fixes (Regular Dev Process)

PR is open in fork as normal, but target branch will be LTS (jakarta) branch

- [Existing] User open's PR as normal for fix, etc <https://jenkins.edgexfoundry.org/blue/organizations/jenkins/edgexfoundry%2Fsample-service/detail/PR-135/8/pipeline>
  - [New] getGolangBaseImage will return Go LTS image that DevOps manually releases into Nexus release

    ```bash
    docker build -t ci-base-image-x86_64 -f Dockerfile --build-arg BASE=nexus3.edgexfoundry.org:10002/edgex-devops/edgex-golang-base:1.16-alpine-lts --build-arg 'MAKE=echo noop' --target=builder .
    ```
