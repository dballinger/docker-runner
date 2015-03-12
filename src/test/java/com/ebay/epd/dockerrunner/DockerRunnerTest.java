package com.ebay.epd.dockerrunner;

import org.junit.AfterClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class DockerRunnerTest {

    private static final DockerRunner dockerRunner = new DockerRunner();


    @AfterClass
    public static void afterClass() {
        dockerRunner.stopAll();
    }

    @Test
    public void shouldStartASimpleContainer() throws Exception {
        Container container = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
//        int status = client.target(url).request().get().getStatus();
//        assertThat(status, is(200));
        fail();
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainer() throws Exception {
        Container container = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        startedContainer.stop();
        String url = String.format("http://%s:%s/ok", host, port);
//        client.target(url).request().get().getStatus();
        fail();
    }

    @Test(expected = Exception.class)
    public void shouldStopAContainerUsingTheRunner() throws Exception {
        Container container = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        dockerRunner.stopAll();
        String url = String.format("http://%s:%s/ok", host, port);
//        client.target(url).request().get().getStatus();
        fail();
    }

    @Test
    public void shouldStartALinkedContainer() throws Exception {
        Container upstream = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        Container proxy = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-proxy").linkTo(upstream).withAlias("root").build();
        StartedContainer startedProxy = proxy.start(blockUntilHttpGetReturns200(), 3);
        String host = dockerRunner.host();
        int port = startedProxy.tcpPort(80);
        String url = String.format("http://%s:%s/ok", host, port);
//        int status = client.target(url).request().get().getStatus();
//        assertThat(status, is(200));
        fail();
    }

    @Test
    public void shouldProvideAStartedLinkedContainer() throws Exception {
        Container containerA = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        Container containerB = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1").build();
        Container containerRequiringAandB = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-image1")
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
        Container container = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-delayed-startup").build();
        StartedContainer startedContainer = container.start(blockUntilHttpGetReturns200(), 7);
        String host = dockerRunner.host();
        int port = startedContainer.tcpPort(80);
        String url = String.format("http://%s:%s", host, port);
//        int status = client.target(url).request().get().getStatus();
//        assertThat(status, is(200));
        fail();
    }

    @Test(expected = Container.ContainerStartupTimeoutException.class)
    public void shouldTimeoutIfUnableToMeetInitialConditionWithinTimeframe() throws Exception {
        //This image sleeps for 5 seconds before starting the webserver.
        Container container = dockerRunner.containerFor("commregistry-slc.corp.ebay.com/spartans/docker-runner-delayed-startup").build();
        container.start(blockUntilHttpGetReturns200(), 1);
    }

    @Test
    public void shouldShutdownStartedAndLinkedContainersInCaseOfAStartupTimeout() throws Exception {
        fail();
    }

    private BlockUntil blockUntilHttpGetReturns200() {
        return new BlockUntil() {
            @Override
            public boolean conditionMet(DockerContext context) {
                throw new UnsupportedOperationException();
//                return client.target(String.format("http://%s:%s", context.host(), context.container().tcpPort(80))).request().get().getStatus() == 200;
            }
        };
    }
}
