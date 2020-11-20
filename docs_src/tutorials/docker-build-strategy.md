# Docker Build Strategy

Docker is a great tool for the CI/CD ecosystem. As the EdgeX DevOps community, we made the decision early when designing the EdgeX Jenkins pipelines to make Docker the center of our build strategy. The advantage of using Docker as the build context is that it allows for creating reproducible builds from Jenkins to our local build environment. Thus creating a portable build environment and minimizing build errors between environments. The EdgeX Jenkins pipelines use Docker images as the context for any build. This Docker image is generated from the `Dockerfile.build` found at the root of every EdgeX repository. Any dependencies or packages required for testing or compilation needs to be added to the `Dockerfile.build` file.

## Local Testing

If we want to test how the build will run on Jenkins we can follow these steps locally.

### Example

First we build the "build image" `edgex-ci-build-image`

```bash
cd app-service-configurable
docker build -t edgex-ci-build-image -f Dockerfile.build
```

Now we run the build image with some make targets and bind mount our current directory to a folder called `/ws` (workspace)

```bash
docker run --rm -v $(pwd):/ws -w /ws edgex-ci-build-image sh -c 'make test build'
```

Or to put it into a convenient one-liner:

```bash
cd app-service-configurable
docker build -t edgex-ci-build-image -f Dockerfile.build . && docker run --rm -v $(pwd):/ws -w /ws edgex-ci-build-image sh -c 'make test build'
```

```plaintext
Sending build context to Docker daemon    127kB
Step 1/8 : ARG BASE=golang:1.15-alpine
Step 2/8 : FROM ${BASE}
 ---> 1a87ceb1ace5
Step 3/8 : LABEL license='SPDX-License-Identifier: Apache-2.0'   copyright='Copyright (c) 2019: Intel'
 ---> Using cache
 ---> 9f1aa172c1d7
Step 4/8 : RUN sed -e 's/dl-cdn[.]alpinelinux.org/nl.alpinelinux.org/g' -i~ /etc/apk/repositories
 ---> Using cache
 ---> fec4da09e9ec
Step 5/8 : RUN apk add --no-cache make git gcc libc-dev libsodium-dev zeromq-dev bash
...
Successfully built ce2be0b9fe31
Successfully tagged edgex-ci-build-image:latest
CGO_ENABLED=1 go test -coverprofile=coverage.out ./...
?   github.com/edgexfoundry/app-service-configurable
    [no test files]
CGO_ENABLED=1 go build -ldflags "-X github.com/edgexfoundry/app-functions-sdk-go/internal.SDKVersion=v1.2.1-dev.35 -X github.com/edgexfoundry/app-functions-sdk-go/internal.ApplicationVersion=0.0.0" app-service-configurable
```

## Tooling Caveats

Docker build images are Alpine based to save on disk space and bandwith and with that comes potential tooling incompatiblities. For example a number of pre-installed base packages on Alpine are the BusyBox versions of the tools. BusyBox versions can sometimes have different arguments than their GNU counterparts. For instance the `tar` command:

### BusyBox (Alpine)

```bash
$ tar --help
BusyBox v1.31.1 () multi-call binary.

Usage: tar c|x|t [-ZzJjahmvokO] [-f TARFILE] [-C DIR] [-T FILE] [-X FILE] [--exclude PATTERN]... [FILE]...
```

### GNU (Other linux distros)

```bash
$ tar --help
Usage: tar [OPTION...] [FILE]...
GNU 'tar' saves many files together into a single tape or disk archive, and can
restore individual files from the archive.
```

This can lead to unexpected issues if say, for instance, you are depending on a specific flag provided by the tool. One option to fix this is to just use the BusyBox flags, however this may break when not running inside the Docker build image. Another option is to find the alternative package and install that version. For example, Alpine provides the GNU alternative `tar` binary under the `tar` Alpine package:

```bash
$ apk add --update tar
$ tar --help
Usage: tar [OPTION...] [FILE]...
GNU 'tar' saves many files together into a single tape or disk archive, and can
restore individual files from the archive.
```

## The Jenkins Way

The above example is similar to how Jenkins runs the build with a few distinctions. First, the make test and make build commands are broken up into two stages. This is an important distinction because it allows for a more granular pipeline allowing for better error handling. The other distinction is that Jenkins takes advantage of a caching base layer image that is passed in at build time. Take a look at the Dockerfile.build. You will notice the `BASE` docker `ARG` at the top of the file.

```Dockerfile
ARG BASE=golang:1.15-alpine
FROM ${BASE}
...
```

This allows Jenkins to override the base image during the build with an image from Nexus helping to alleviate issues with DockerHub pull limits as well as random Docker pull failures. On Jenkins this happens in the Prep stage:

```bash
docker build -t ci-base-image-x86_64 \
  -f Dockerfile.build \
  --build-arg BASE=nexus3.edgexfoundry.org:10003/edgex-devops/edgex-golang-base:1.15-alpine \
  .
```

The DevOps WG team manages these Golang base images and the Dockerfile for the latest Golang image used can be found here: <https://github.com/edgexfoundry/ci-build-images/tree/golang-1.15>. This cache image contains most of the dependencies used in the majority of the pipelines allowing us to cache dependencies at the base image level and increasing builds speeds.

After the base image is built the test and build stages run in a similar manner to the local testing scenario:

```plaintext
docker run -t -u 0:0 \
  -w /w/workspace/app-service-configurable/60 \
  -v /w/workspace/app-service-configurable/60:/w/workspace/app-service-configurable/60:rw,z \
  -v /w/workspace/app-service-configurable/60@tmp:/w/workspace/app-service-configurable/60@tmp:rw,z \
  ci-base-image-x86_64 ... make test
```

## Next steps

More information can be found by reading the documentation or source code of these pipelines:

- [edgeXBuildGoApp](https://edgexfoundry.github.io/edgex-global-pipelines/html/libraries/edgeXBuildGoApp/) [source](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/vars/edgeXBuildGoApp.groovy)
- [edgeXBuildGoParallel](https://edgexfoundry.github.io/edgex-global-pipelines/html/libraries/edgeXBuildGoParallel/) [source](https://github.com/edgexfoundry/edgex-global-pipelines/blob/master/vars/edgeXBuildGoParallel.groovy)