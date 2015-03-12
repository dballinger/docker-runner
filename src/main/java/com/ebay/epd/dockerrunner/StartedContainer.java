package com.ebay.epd.dockerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

import java.util.Collection;
import java.util.Map;

public class StartedContainer {

    private final String id;
    private final Map<String, StartedContainer> linkedContainers;
    private final DockerClient client;

    public StartedContainer(DockerClient client, String id, Map<String, StartedContainer> linkedContainers) {
        this.client = client;
        this.id = id;
        this.linkedContainers = linkedContainers;
    }

    public int tcpPort(int containerPort) {
        InspectContainerResponse inspectContainerResponse = client.inspectContainerCmd(id).exec();
        Ports.Binding binding = inspectContainerResponse.getNetworkSettings().getPorts().getBindings().get(ExposedPort.tcp(containerPort))[0];
        return binding.getHostPort();
    }

    public void stop() {
        client.stopContainerCmd(id).withTimeout(1).exec();
    }

    public String name() {
        return client.inspectContainerCmd(id).exec().getName();
    }

    public StartedContainer linkedContainerWithAlias(String alias) {
        return linkedContainers.get(alias);
    }

    Collection<StartedContainer> linkedContainers() {
        return linkedContainers.values();
    }

//    public HostConfig hostConfig() {
//        return hostConfig;
//    }
}
