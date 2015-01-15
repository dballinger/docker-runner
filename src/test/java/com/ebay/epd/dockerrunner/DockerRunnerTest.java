package com.ebay.epd.dockerrunner;

import com.spotify.docker.client.shaded.javax.ws.rs.client.Client;
import com.spotify.docker.client.shaded.javax.ws.rs.client.ClientBuilder;
import org.junit.AfterClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DockerRunnerTest {

    private static final DockerRunner dockerRunner = new DockerRunner();

    private final Client client = ClientBuilder.newClient();

    @AfterClass
    public static void afterClass() {
        dockerRunner.stopAll();
    }

    @Test
    public void shouldStartASimpleContainer() throws Exception {
        Container container = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(),3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
        int status = client.target(url).request().get().getStatus();
        assertThat(status, is(200));
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainer() throws Exception {
        Container container = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(),3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        startedContainer.stop();
        String url = String.format("http://%s:%s/ok", host, port);
        client.target(url).request().get().getStatus();
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainerUsingTheRunner() throws Exception {
        Container container = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(),3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        dockerRunner.stopAll();
        String url = String.format("http://%s:%s/ok", host, port);
        client.target(url).request().get().getStatus();
    }

    @Test
    public void shouldStartALinkedContainer() throws Exception {
        Container upstream = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        Container proxy = dockerRunner.containerFor("spartans/docker-runner-proxy").linkTo(upstream).withAlias("root").build();
        StartedContainer startedProxy = proxy.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedProxy.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
        int status = client.target(url).request().get().getStatus();
        assertThat(status, is(200));
    }

    @Test
    public void shouldProvideAStartedLinkedContainer() throws Exception {
        Container containerA = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        Container containerB = dockerRunner.containerFor("spartans/docker-runner-image1").build();
        Container containerRequiringAandB = dockerRunner.containerFor("spartans/docker-runner-image1")
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
        Container container = dockerRunner.containerFor("spartans/docker-runner-delayed-startup").build();
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
        Container container = dockerRunner.containerFor("spartans/docker-runner-delayed-startup").build();
        container.start(blockUntilHttpGetReturns200(), 1);
    }

    private BlockUntil blockUntilHttpGetReturns200() {
        return new BlockUntil() {
            @Override
            public boolean conditionMet(String host, StartedContainer container) {
                return client.target(String.format("http://%s:%s", host, container.tcpPort(80))).request().get().getStatus() == 200;
            }
        };
    }
}
