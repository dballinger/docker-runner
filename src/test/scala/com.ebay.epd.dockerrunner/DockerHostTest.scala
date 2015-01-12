package com.ebay.epd.dockerrunner

import java.util

import com.ebay.epd.dockerrunner.DockerHostFactory.{NativeDockerHost, DockerHostException, BootToDockerHost}
import com.spotify.docker.client.messages.Info
import org.scalatest.{Matchers, FlatSpec}

class DockerHostTest extends FlatSpec with Matchers {

  "Docker host factory" should "return non TLS B2D host" in {
    val env = new util.HashMap[String, String]()
    env.put("DOCKER_HOST", "tcp://1.2.3.4:1234")
    val dockerHost = DockerHostFactory.dockerHostForEnvironment(env)
    dockerHost.isInstanceOf[BootToDockerHost] should be(true)
    dockerHost.host() should be("1.2.3.4")
    dockerHost.client() == null should be(false)
  }

  it should "return native host" in {
    val env = new util.HashMap[String, String]()
    val dockerHost = DockerHostFactory.dockerHostForEnvironment(env)
    dockerHost.isInstanceOf[NativeDockerHost] should be(true)
    dockerHost.host() should be("localhost")
    dockerHost.client() == null should be(false)
  }

  it should "throw if DOCKER_HOST is unvalid URI" in {
    val env = new util.HashMap[String, String]()
    env.put("DOCKER_HOST", "not-valid-uri")
    intercept[DockerHostException] {
      DockerHostFactory.dockerHostForEnvironment(env)
    }
  }
}