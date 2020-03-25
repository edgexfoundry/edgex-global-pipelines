# How to test

Ensure the current working directory is the root of the edgex-global-pipelines repository.

## Testing with Native Gradle

Gradle easily allows for running without manually needing to install the gradle binary. The provided wrapper script gradlew/gradlew.bat will download gradle and run whatever task you specify.

```Shell
./gradlew clean test
```

## Testing with Dockerized Gradle

By running the tests with docker, you avoid having to install gradle on your host machine (Good for CI/CD)

```Shell
docker run --rm -t \
  -v $HOME/.gradle:/home/gradle/.gradle \ # bind mount your gradle cache
  -v $PWD:/code -w /code \ # bind mount current working directory to /code
  gradle:6.2.2 \ # docker image
  gradle clean test # gradle tasks
```

## Running behind a proxy

Gradle allows you to pass in proxy configuration as either command line arguments or the environment variable `GRADLE_OPTS`.

### Command line example

```Shell
gradle -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=1234 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=1234 test
```

### Environment variable example

```Shell
export GRADLE_OPTS=-Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=1234 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=1234
gradle test
```

### Environment variable with docker example

```Shell
docker run --rm -t \
  -e GRADLE_OPTS="-Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=1234 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=1234" \
  -v $HOME/.gradle:/home/gradle/.gradle \ # bind mount your gradle cache
  -v $PWD:/code -w /code \ # bind mount current working directory to /code
  gradle:6.2.2 \ # docker image
  gradle clean test # gradle tasks
```

**Note:** The initial compilation takes several minutes because Gradle downloads all the dependencies defined in the build.gradle. After the initial run subsequent executions for testing should be quicker.
