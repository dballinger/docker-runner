package com.ebay.epd.dockerrunner

import java.util

import com.ebay.epd.dockerrunner.DockerHostFactory._
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

  it should "return TLS B2D host" in {
    val env = new util.HashMap[String, String]()
    env.put("DOCKER_HOST", "tcp://1.2.3.4:1234")
    env.put("DOCKER_TLS_VERIFY", "1")
    env.put("DOCKER_CERT_PATH", ".")
    /*
    Yeah, this is a bad test. DockerHostCertificateException only gets thrown from inside the TlsHost.
    Unfortunately I can't find a suitable test without committing a real b2d pem.
    Someone please improve this.
     */
    intercept[DockerHostCertificateException] {
      val dockerHost = DockerHostFactory.dockerHostForEnvironment(env)
    }
  }

  it should "throw if DOCKER_HOST is unvalid URI" in {
    val env = new util.HashMap[String, String]()
    env.put("DOCKER_HOST", "not-valid-uri")
    intercept[DockerHostException] {
      DockerHostFactory.dockerHostForEnvironment(env)
    }
  }
}