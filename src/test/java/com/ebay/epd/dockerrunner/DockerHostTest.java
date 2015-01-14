package com.ebay.epd.dockerrunner;

import org.junit.Test;

import java.util.Map;

import static jersey.repackaged.com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class DockerHostTest {

    @Test
    public void shouldReturnNonTlsB2DHost() throws Exception {
        Map<String, String> env = newHashMap();
        env.put("DOCKER_HOST", "tcp://1.2.3.4:1234");
        DockerHostFactory.DockerHost dockerHost = DockerHostFactory.dockerHostForEnvironment(env);
        assertThat(dockerHost, is(instanceOf(DockerHostFactory.BootToDockerHost.class)));
        assertThat(dockerHost.host(), is("1.2.3.4"));
        assertThat(dockerHost.client(), is(not(nullValue())));
    }

    @Test
    public void shouldReturnANativeHost() throws Exception {
        Map<String, String> env = newHashMap();
        DockerHostFactory.DockerHost dockerHost = DockerHostFactory.dockerHostForEnvironment(env);
        assertThat(dockerHost, is(instanceOf(DockerHostFactory.NativeDockerHost.class)));
        assertThat(dockerHost.host(), is("localhost"));
        assertThat(dockerHost.client(), is(not(nullValue())));
    }

    @Test(expected = DockerHostFactory.DockerHostCertificateException.class)
    public void shouldReturnATlsB2DHost() throws Exception {
        /*
            Yeah, this is a bad test. DockerHostCertificateException only gets thrown from inside the TlsHost.
            Unfortunately I can't find a suitable test without committing a real b2d pem.
            Someone please improve this.
             */
        Map<String, String> env = newHashMap();
        env.put("DOCKER_HOST", "tcp://1.2.3.4:1234");
        env.put("DOCKER_TLS_VERIFY", "1");
        env.put("DOCKER_CERT_PATH", ".");
        DockerHostFactory.dockerHostForEnvironment(env);
    }

    @Test(expected = DockerHostFactory.DockerHostException.class)
    public void shouldThrowIfUriIsInvalid() throws Exception {
        Map<String, String> env = newHashMap();
        env.put("DOCKER_HOST", "invalid-uri");
        DockerHostFactory.dockerHostForEnvironment(env);
    }
}
