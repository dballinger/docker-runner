package com.ebay.epd.dockerrunner

import dispatch._
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Second, Span}

import scala.concurrent.Await

class DockerRunnerTest extends FlatSpec with Eventually with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global
  import concurrent.duration._

  override implicit val patienceConfig = PatienceConfig(timeout = Span(1, Second))

  "Docker runner" should "start a simple container" in {
    val dockerRunner = new DockerRunner()
    val container = dockerRunner.newContainer("spartans/docker-runner-image1")
    val startedContainer = container.start()
    val host = dockerRunner.host()
    val port = startedContainer.tcpPort(80)
    eventually {
      val body = Await.result(Http(url(s"http://$host:$port/ok") OK as.String), 1 second)
      body should be("ok")
    }
  }

  "Docker runner" should "stop a simple container" in {
    val dockerRunner = new DockerRunner()
    val container = dockerRunner.newContainer("spartans/docker-runner-image1")
    val startedContainer = container.start()
    val host = dockerRunner.host()
    val port = startedContainer.tcpPort(80)
    startedContainer.stop()
    eventually {
      intercept[Exception] {
        Await.result(Http(url(s"http://$host:$port/ok") OK as.String), 1 second)
      }
    }
  }

}