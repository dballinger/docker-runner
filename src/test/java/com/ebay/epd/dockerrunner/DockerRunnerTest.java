package com.ebay.epd.dockerrunner;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DockerRunnerTest {

    private static final DockerRunner dockerRunner = new DockerRunner();

    private Client client = null;

    @AfterClass
    public static void afterClass() {
        dockerRunner.stopAll();
    }

    @Before
    public void before() {
        client = ClientBuilder.newClient();
    }

    @Test
    public void shouldStartASimpleContainer() throws Exception {
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
        int status = client.target(url).request().get().getStatus();
        assertThat(status, is(200));
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainer() throws Exception {
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        startedContainer.stop();
        String url = String.format("http://%s:%s/ok", host, port);
        client.target(url).request().get().getStatus();
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainerUsingTheRunner() throws Exception {
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        dockerRunner.stopAll();
        String url = String.format("http://%s:%s/ok", host, port);
        client.target(url).request().get().getStatus();
    }

    @Test
    public void shouldStartALinkedContainer() throws Exception {
        Container upstream = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        Container proxy = dockerRunner.containerFor("dockerrunner/docker-runner-proxy:v1").linkTo(upstream).withAlias("root").build();
        StartedContainer startedProxy = proxy.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedProxy.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
        int status = client.target(url).request().get().getStatus();
        assertThat(status, is(200));
    }

    @Test
    public void shouldProvideAStartedLinkedContainer() throws Exception {
        Container containerA = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        Container containerB = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1").build();
        Container containerRequiringAandB = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1")
                                             .linkTo(containerA).withAlias("a")
                                             .linkTo(containerB).withAlias("b")
                                             .build();
        StartedContainer startedContainerRequiringAandB = containerRequiringAandB.start();
        StartedContainer resolvedStartedContainerA = startedContainerRequiringAandB.linkedContainerWithAlias("a");
        StartedContainer resolvedStartedContainerB = startedContainerRequiringAandB.linkedContainerWithAlias("b");

        assertThat(resolvedStartedContainerA, is(sameInstance(containerA.start())));
        assertThat(resolvedStartedContainerB, is(sameInstance(containerB.start())));
    }

    @Test
    public void shouldWaitForAInitialConditionToBeMet() throws Exception {
        //This image sleeps for 5 seconds before starting the webserver.
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-delayed-startup:v1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 7);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s", host, port);
        int status = client.target(url).request().get().getStatus();
        assertThat(status, is(200));
    }

    @Test(expected = Container.ContainerStartupTimeoutException.class)
    public void shouldTimeoutIfUnableToMeetInitialConditionWithinTimeframe() throws Exception {
        //This image sleeps for 5 seconds before starting the webserver.
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-delayed-startup:v1").build();
        container.start(blockUntilHttpGetReturns200(), 1);
    }

    @Test
    public void shouldPassEnvVarsToContainer() throws Exception {
        String expectedValue = UUID.randomUUID().toString();
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1")
                               .env("TEST_VAR", expectedValue)
                               .build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/vars.txt", host, port);
        String actualValue = client.target(url).request().get().readEntity(String.class);
        assertThat(actualValue, containsString(expectedValue));
    }

    @Test
    public void shouldPassDnsToContainer() throws Exception {
        String expectedDns = "0.1.2.3";
        Container container = dockerRunner.containerFor("dockerrunner/docker-runner-image1:v1")
                               .dns(expectedDns)
                               .build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/dns.txt", host, port);
        String actualDns = client.target(url).request().get().readEntity(String.class);
        assertThat(actualDns, containsString(expectedDns));
    }

    @Test
    public void shouldMountAVolume() throws Exception {
        String expectedContent = "some content";

        StartedContainer startedContainer = dockerRunner.containerFor("nginx")
                                             .mountHostVolume(new File("src/test/resources/volumes").getAbsolutePath()).toContainerVolume("/usr/share/nginx/html")
                                             .build()
                                             .start();
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/content", host, port);
        String actualContent = client.target(url).request().get().readEntity(String.class);

        assertThat(actualContent, containsString(expectedContent));
    }

    @Test
    public void shouldExposeAdditionalPorts() throws Exception {
        int additonalPort = 12345;
        StartedContainer startedContainer = dockerRunner.containerFor("nginx")
                                             .exposing(additonalPort)
                                             .mountHostVolume(new File("src/test/resources/nginx-12345-port/nginx.conf").getAbsolutePath()).toContainerVolume("/etc/nginx/nginx.conf")
                                             .build()
                                             .start();
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(additonalPort);
        String url = String.format("http://%s:%s", host, port);
        int status = client.target(url).request().get().getStatus();

        assertThat(status, is(200));
    }

    private BlockUntil blockUntilHttpGetReturns200() {
        return new BlockUntil() {
            @Override
            public boolean conditionMet(DockerContext context) {
                return client.target(String.format("http://%s:%s", context.host(), context.container().tcpPort(80))).request().get().getStatus() == 200;
            }
        };
    }
}
