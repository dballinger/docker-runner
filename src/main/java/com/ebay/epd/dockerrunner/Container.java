package com.ebay.epd.dockerrunner;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;

public class Container {
    private DockerClient client;
    private String imageName;

    public Container(DockerClient client, String imageName) {
        this.client = client;
        this.imageName = imageName;
    }

    public StartedContainer start() {
        HostConfig hostConfig = HostConfig.builder().publishAllPorts(true).build();
        ContainerConfig containerConfig = ContainerConfig.builder().image(imageName).build();
        ContainerCreation container = null;
        try {
            container = client.createContainer(containerConfig);
            client.startContainer(container.id(), hostConfig);
        } catch (DockerException | InterruptedException e) {
            throw new ContainerException(e);
        }
        return new StartedContainer(client, container.id());
    }

    public class StartedContainer {

        private final DockerClient client;
        private final String id;

        public StartedContainer(DockerClient client, String id) {
            this.client = client;
            this.id = id;
        }

        public int tcpPort(int containerPort) {
            try {
                String portString = client.inspectContainer(id).networkSettings().ports().get(String.format("%s/tcp", containerPort)).get(0).hostPort();
                return Integer.parseInt(portString);
            } catch (DockerException | InterruptedException e) {
                throw new ContainerException(e);
            }
        }

        public void stop() {
            try {
                client.stopContainer(id, 1);
            } catch (DockerException | InterruptedException e) {
                throw new ContainerException(e);
            }
        }
    }

    public static class ContainerException extends RuntimeException {
        public ContainerException(Exception e) {
            super(e);
        }
    }
}
