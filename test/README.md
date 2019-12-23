## How to test

Ensure the current working directory is the root of the edgex-global-pipelines repository.

If you are running behind a proxy, then you will need to bind mount the settings.xml file. Note their is an issue with Maven interpolating the proxy port thus you will need to update the PROXY_PORT value in the settings.xml manually; the default port value is 911.

```Shell
docker container run \
--rm \
--env PROXY_HOST="--specify-your-proxy-server-hostname-here--" \
-it \
-v $(pwd):/edgex-global-pipelines \
-v $(pwd)/test/settings.xml:/root/.m2/settings.xml \
-w /edgex-global-pipelines \
maven:3.6.3-jdk-8 \
/bin/sh
```

Execute the tests with the following command:
```Shell
mvn clean test
```

Note: The initial compilation takes several minutes because Maven downloads all the dependencies defined in the pom.xml. After the initial run subsequent test executions should execute quicker.