Docker Runner
=============

Docker Runner is a little Java wrapper for Docker, specifically for use in tests. It provides a subset of the docker functionality in a fluent DSL. [docker-java](https://github.com/docker-java/docker-java) is used for connecting to the Docker daemon.

While the set of functionality is fairly small, it targets common use cases that we need in out tests at eBay's European Product & Development.

Please look at [DockerRunnerTest.java](https://github.com/eBay-European-Product-Development/docker-runner/blob/master/src/test/java/com/ebay/epd/dockerrunner/DockerRunnerTest.java) for its usage.

Prerequistites
--------------
On linux this should just work out of the box. However, if you are on a Boot2Docker environment then you must ensure that the Docker environment variable are set in the shell or IDE from which you are using the library. The easiest way to do this is to run the flavour of ```boot2docker shellinit``` for your chosen platform. Take a look at [Windows](https://docs.docker.com/installation/windows/) or [Mac OSX](https://docs.docker.com/installation/mac/).
