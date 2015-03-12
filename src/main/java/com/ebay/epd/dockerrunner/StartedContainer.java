package com.ebay.epd.dockerrunner;

import java.util.Collection;
import java.util.Map;

public class StartedContainer {

    private final String id;
    private final Map<String, StartedContainer> linkedContainers;

    public StartedContainer(String id, Map<String, StartedContainer> linkedContainers) {
        this.id = id;
        this.linkedContainers = linkedContainers;
    }

    public int tcpPort(int containerPort) {
//        try {
//            String portString = client.inspectContainer(id).networkSettings().ports().get(String.format("%s/tcp", containerPort)).get(0).hostPort();
//            return Integer.parseInt(portString);
//        } catch (DockerException | InterruptedException e) {
//            throw new Container.ContainerException(e);
//        }
        throw new UnsupportedOperationException();
    }

    public void stop() {
//        try {
//            client.stopContainer(id, 1);
//        } catch (DockerException | InterruptedException e) {
//            throw new Container.ContainerException(e);
//        }
    }

    public String name() {
//        try {
//            return client.inspectContainer(id).name();
//        } catch (DockerException | InterruptedException e) {
//            throw new Container.ContainerException(e);
//        }
        throw new UnsupportedOperationException();
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
