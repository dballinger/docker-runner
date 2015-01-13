package com.ebay.epd.dockerrunner;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class Container {
    private DockerClient client;
    private String imageName;
    private Iterable<Link> links;
    private StartedContainer startedContainer;

    public Container(DockerClient client, String imageName, Iterable<Link> links) {
        this.client = client;
        this.imageName = imageName;
        this.links = links;
    }

    public StartedContainer start() {
        if (startedContainer != null) {
            return startedContainer;
        } else {
            List<String> concatLinks = newArrayList(Iterables.transform(links, new Function<Link, String>() {
                @Override
                public String apply(Link link) {
                    return String.format("%s:%s", link.name, link.alias);
                }
            }));
            HostConfig hostConfig = HostConfig.builder().publishAllPorts(true).links(concatLinks).build();
            ContainerConfig containerConfig = ContainerConfig.builder().image(imageName).build();
            ContainerCreation container;
            try {
                container = client.createContainer(containerConfig);
                client.startContainer(container.id(), hostConfig);
            } catch (DockerException | InterruptedException e) {
                throw new ContainerException(e);
            }
            startedContainer = new StartedContainer(client, container.id());
            return startedContainer;
        }
    }

    void stopIfStarted() {
        if (startedContainer != null) {
            startedContainer.stop();
        }
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

        public String name() {
            try {
                return client.inspectContainer(id).name();
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
