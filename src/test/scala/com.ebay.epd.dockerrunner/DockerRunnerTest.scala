package com.ebay.epd.dockerrunner

import dispatch._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Second, Span}

import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class DockerRunnerTest extends FlatSpec with Eventually with Matchers with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global
  import concurrent.duration._

  override implicit val patienceConfig = PatienceConfig(timeout = Span(2, Seconds))

  val dockerRunner = new DockerRunner()

  override protected def afterAll(): Unit = {
    dockerRunner.stopAll()
    super.afterAll()
  }

  "Docker runner" should "start a simple container" in {
    val container = dockerRunner.containerFor("spartans/docker-runner-image1").build()
    val startedContainer = container.start()
    val host = dockerRunner.host()
    val port = startedContainer.tcpPort(80)
    eventually {
      val body = Await.result(Http(url(s"http://$host:$port/ok") OK as.String), 1 second)
      body should be("ok")
    }
  }

  it should "stop a simple container" in {
    val container = dockerRunner.containerFor("spartans/docker-runner-image1").build()
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

  it should "stop a simple container using the runner" in {
    val container = dockerRunner.containerFor("spartans/docker-runner-image1").build()
    val startedContainer = container.start()
    val host = dockerRunner.host()
    val port = startedContainer.tcpPort(80)
    dockerRunner.stopAll()
    eventually {
      intercept[Exception] {
        Await.result(Http(url(s"http://$host:$port/ok") OK as.String), 1 second)
      }
    }
  }

  it should "start a linked container" in {
    val upstream = dockerRunner.containerFor("spartans/docker-runner-image1").build()
    val proxy = dockerRunner.containerFor("spartans/docker-runner-proxy").linkTo(upstream).withAlias("root").build()
    val startedProxy = proxy.start()
    val host = dockerRunner.host()
    val port = startedProxy.tcpPort(80)
    eventually {
      val body = Await.result(Http(url(s"http://$host:$port/ok") OK as.String), 1 second)
      body should be("ok")
    }
  }

}