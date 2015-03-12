package com.ebay.epd.dockerrunner;

public class ContainerTest {

//    private final DockerHostFactory.DockerHost dockerHost = new DockerHostFactory().dockerHostForEnvironment(System.getenv());
//    private final DockerClient client = dockerHost.client();
//
//    @Test
//    public void shouldStartAndStopAContainer() throws Exception {
//        Container container = new Container(client, "commregistry-slc.corp.ebay.com/spartans/docker-runner-image1", Lists.<Link>newArrayList(), dockerHost.host(), Option.<String>None(), Option.<Memory>None(), Lists.<Container.Env>newArrayList(), Option.<String>None());
//        int initialNumberOfRunningContainers = client.listContainers().size();
//        StartedContainer startedContainer = container.start();
//        int numberOfRunningContainersAfterStart = client.listContainers().size();
//        startedContainer.stop();
//        int finalNumberOfRunningContainers = client.listContainers().size();
//        assertThat(numberOfRunningContainersAfterStart, is(initialNumberOfRunningContainers + 1));
//        assertThat(finalNumberOfRunningContainers, is(initialNumberOfRunningContainers));
//    }
//
//    @Test
//    public void shouldOnlyStartContainerOnce() throws Exception {
//        Container container = new Container(client, "commregistry-slc.corp.ebay.com/spartans/docker-runner-image1", Lists.<Link>newArrayList(), dockerHost.host(), Option.<String>None(), Option.<Memory>None(), Lists.<Container.Env>newArrayList(), Option.<String>None());
//        StartedContainer startedContainer1 = container.start();
//
//        int numberOfRunningContainersAfterFirstStart = client.listContainers().size();
//        StartedContainer startedContainer2 = container.start();
//        int numberOfRunningContainersAfterSecondStart = client.listContainers().size();
//        startedContainer1.stop();
//        startedContainer2.stop();
//        int finalNumberOfRunningContainers = client.listContainers().size();
//
//        assertThat(numberOfRunningContainersAfterFirstStart, is(numberOfRunningContainersAfterSecondStart));
//        assertThat(finalNumberOfRunningContainers, is(numberOfRunningContainersAfterSecondStart - 1));
//        assertThat(startedContainer1, is(startedContainer2));
//    }
}
