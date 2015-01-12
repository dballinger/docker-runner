package com.ebay.epd.dockerrunner

import com.ebay.epd.dockerrunner.DockerRunner.Link
import org.scalatest.{Matchers, FlatSpec, FunSuite}

class ContainerTest extends FlatSpec with Matchers {

  import collection.JavaConversions._

  "Container" should "start and stop" in {
    val client = DockerHostFactory.dockerHostForEnvironment(System.getenv()).client
    val container = new Container(client, "spartans/docker-runner-image1", List[Link]())
    val initialNumberOfRunningContainers = client.listContainers().size()
    val startedContainer = container.start()
    val numberOfRunningContainersAfterStart = client.listContainers().size()
    startedContainer.stop()
    val finalNumberOfRunningContainers = client.listContainers().size()

    numberOfRunningContainersAfterStart should be(initialNumberOfRunningContainers + 1)
    finalNumberOfRunningContainers should be(initialNumberOfRunningContainers)
  }

  it should "only start once" in {
    val client = DockerHostFactory.dockerHostForEnvironment(System.getenv()).client
    val container = new Container(client, "spartans/docker-runner-image1", List[Link]())

    val startedContainer1 = container.start()
    val numberOfRunningContainersAfterFirstStart = client.listContainers().size()
    val startedContainer2 = container.start()
    val numberOfRunningContainersAfterSecondStart = client.listContainers().size()
    startedContainer1.stop()
    startedContainer2.stop()
    val finalNumberOfRunningContainers = client.listContainers().size()

    numberOfRunningContainersAfterFirstStart should be(numberOfRunningContainersAfterSecondStart)
    finalNumberOfRunningContainers should be(numberOfRunningContainersAfterSecondStart - 1)
    startedContainer1 should be(startedContainer2)
  }
}