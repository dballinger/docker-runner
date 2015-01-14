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
    private final DockerClient client;
    private final String imageName;
    private final Iterable<Link> links;
    private final String host;
    private Option<String> cpuset;
    private Option<Memory> memory;
    private StartedContainer startedContainer;

    private final BlockUntil noBlock = new BlockUntil() {
        @Override
        public boolean conditionMet(String host, StartedContainer container) {
            return true;
        }
    };

    public Container(DockerClient client, String imageName, Iterable<Link> links, String host, Option<String> cpuset, Option<Memory> memory) {
        this.client = client;
        this.imageName = imageName;
        this.links = links;
        this.host = host;
        this.cpuset = cpuset;
        this.memory = memory;
    }

    public StartedContainer start() {
        return start(noBlock);
    }

    public StartedContainer start(BlockUntil blockUntil) {
        if (startedContainer == null) {
            List<String> concatLinks = newArrayList(Iterables.transform(links, new Function<Link, String>() {
                @Override
                public String apply(Link link) {
                    return String.format("%s:%s", link.name, link.alias);
                }
            }));
            HostConfig hostConfig = HostConfig.builder().publishAllPorts(true).links(concatLinks).build();
            final ContainerConfig.Builder containerConfig = ContainerConfig.builder().image(imageName);
            cpuset.doIfPresent(new Option.OptionalCommand<String>() {
                @Override
                public void apply(String cs) {
                    containerConfig.cpuset(cs);
                }
            });
            memory.doIfPresent(new Option.OptionalCommand<Memory>() {
                @Override
                public void apply(Memory mem) {
                    containerConfig.memory(mem.toBytes());
                }
            });

            ContainerCreation container;
            try {
                container = client.createContainer(containerConfig.build());
                client.startContainer(container.id(), hostConfig);
            } catch (DockerException | InterruptedException e) {
                throw new ContainerException(e);
            }
            startedContainer = new StartedContainer(client, container.id());
        }
        waitFor(blockUntil);
        return startedContainer;
    }

    private void waitFor(BlockUntil blockUntil) {
        try {
            if(blockUntil.conditionMet(host, startedContainer)) {
                return;
            }
        } catch (Exception e) {
            //Exception is equivalent to false... carry on.
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //Do we need to worry about this?
        }
        waitFor(blockUntil);
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
