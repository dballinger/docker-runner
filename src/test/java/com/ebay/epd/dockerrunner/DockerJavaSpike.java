package com.ebay.epd.dockerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerJavaSpike {

    public static void main(String[] args) throws InterruptedException {
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri("unix:///var/run/docker.sock")
                .build();
        DockerClient client = DockerClientBuilder.getInstance(config).build();

        String newtoken = client.createContainerCmd("commregistry-slc.corp.ebay.com/spartans/newtoken-mock").exec().getId();
        client.startContainerCmd(newtoken).withPublishAllPorts(true).exec();
        for (Container container : client.listContainersCmd().exec()) {
            System.out.println("CONTAINER");
            for (String name : container.getNames()) {
                System.out.println(name);
            }
        }

        Thread.sleep(30000);
    }
}
