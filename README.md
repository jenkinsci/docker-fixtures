# Overview

A Docker test fixture system available for use from any JUnit tests,
especially using `JenkinsRule` in the [Jenkins functional test harness](https://github.com/jenkinsci/jenkins-test-harness/blob/master/README.md).

The use case is for tests which need to connect to some kind of self-contained (Linux) service
which cannot be easily hosted on the test machine itself.
Typical examples would include Jenkins agents with specific software packages preloaded,
or SCM servers which it would be hard to launch portably from any environment.

Another reason to run some things inside Docker is to simply simulate cases where code runs remotely,
with different filesystem paths, user accounts, etc.

Note that the typical architecture is that of a single machine with a local Docker daemon.
Tests are assumed to be running on the host itself;
i.e., this is unlike a multicontainer test system you might orchestrate with Docker Compose.

Probably this library should be deprecated in favor of [testcontainers.org](https://www.testcontainers.org/).

# Usage

## Defining a fixture

Each fixture is defined in terms of a `DockerContainer` subtype with a `@DockerFixture` annotation.
This type typically exposes various methods needed to interact with the running fixture.

A fixture also needs to define a `Dockerfile` in the resources directory.
If a fixture class is `org/acme/FooContainer.java`,
then by default the definition must be located at `org/acme/FooContainer/Dockerfile`.
(In the case of inner classes, dollar sign (`$`) is replaced with a slash (`/`).)
The same directory may contain other files `ADD`ed to or `COPY`d into the image, as usual with `docker build`.

### Subclassing another fixture

If your fixture extends another one rather than `DockerContainer` directly,
you can inherit behaviors while adding new ones.
In this case the `FROM` directive of your `Dockerfile` should specify the image of the parent,
as detailed in the Javadoc for `DockerFixture`.

## Running a fixture

Simply add to your (JUnit 4) test:

```java
@ClassRule
public static DockerClassRule<MyContainer> docker = new DockerClassRule<>(MyContainer.class);
```

If and when you wish to start using the fixture from a test case, call the `create()` method.
This will launch the container and give you a handle you can use to call fixture methods.

If the test is run on a system which cannot run the `docker` command,
the test will be treated as skipped automatically.

When the test case finishes, the container is stopped and cleaned up automatically.

## Accessing ports

When you specify `ports` in the annotation, you allow services in the fixture to be accessed from the test.
Always use the `ipBound` and `port` methods on each container to determine where to make the actual connection.

## Custom networks
If you want your docker containers to connect to a custom network you can set the environment variable `DOCKER_FIXTURES_NETWORK` to the name of the network you want to use.

## Example

See the `mercurial-plugin` sources for a complete example of defining and using a fixture, including inheritance:
* `src/test/java/hudson/plugins/mercurial/MercurialContainer.java` declares the fixture, along with some helper nethods
* `src/test/resources/hudson/plugins/mercurial/MercurialContainer/Dockerfile` defines the fixture’s contents
* `src/test/java/hudson/plugins/mercurial/MercurialContainerTest.java` demonstrates its usage

# Changelog

See [GitHub Releases](https://github.com/jenkinsci/docker-fixtures/releases).
(For 1.8 and earlier, see the [old changelog](old-changelog.md).
