package com.ebay.epd.dockerrunner;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;

import java.util.Collection;
import java.util.Map;

public class StartedContainer {

    private final DockerClient client;
    private final String id;
    private final Map<String, StartedContainer> linkedContainers;

    public StartedContainer(DockerClient client, String id, Map<String, StartedContainer> linkedContainers) {
        this.client = client;
        this.id = id;
        this.linkedContainers = linkedContainers;
    }

    public int tcpPort(int containerPort) {
        try {
            String portString = client.inspectContainer(id).networkSettings().ports().get(String.format("%s/tcp", containerPort)).get(0).hostPort();
            return Integer.parseInt(portString);
        } catch (DockerException | InterruptedException e) {
            throw new Container.ContainerException(e);
        }
    }

    public void stop() {
        try {
            client.stopContainer(id, 1);
        } catch (DockerException | InterruptedException e) {
            throw new Container.ContainerException(e);
        }
    }

    public String name() {
        try {
            return client.inspectContainer(id).name();
        } catch (DockerException | InterruptedException e) {
            throw new Container.ContainerException(e);
        }
    }

    public StartedContainer linkedContainerWithAlias(String alias) {
        return linkedContainers.get(alias);
    }

    Collection<StartedContainer> linkedContainers() {
        return linkedContainers.values();
    }
}
