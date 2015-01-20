package com.ebay.epd.dockerrunner;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class Container {
    private final DockerClient client;
    private final String imageName;
    private final Iterable<Link> links;
    private final String host;
    private Option<String> cpuset;
    private Option<Memory> memory;
    private List<Env> envs;
    private StartedContainer startedContainer;

    private final BlockUntil noBlock = new BlockUntil() {
        @Override
        public boolean conditionMet(DockerContext context) {
            return true;
        }
    };
    private String id;

    public Container(DockerClient client, String imageName, Iterable<Link> links, String host, Option<String> cpuset, Option<Memory> memory, List<Env> envs) {
        this.client = client;
        this.imageName = imageName;
        this.links = links;
        this.host = host;
        this.cpuset = cpuset;
        this.memory = memory;
        this.envs = envs;
    }

    public StartedContainer start() {
        return start(noBlock, 0);
    }

    public StartedContainer start(BlockUntil blockUntil, int secondsTimeout) {
        if (startedContainer == null) {
            List<String> concatLinks = newArrayList(Iterables.transform(links, new Function<Link, String>() {
                @Override
                public String apply(Link link) {
                    return String.format("%s:%s", link.container.name(), link.alias);
                }
            }));
            List<String> envStrs = Lists.transform(envs, new Function<Env, String>() {
                @Override
                public String apply(Env input) {
                    return String.format("%s=%s", input.key, input.value);
                }
            });
            HostConfig hostConfig = HostConfig.builder().publishAllPorts(true).links(concatLinks).build();
            final ContainerConfig.Builder containerConfig = ContainerConfig.builder().image(imageName).env(envStrs);
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
                id = container.id();
                client.startContainer(id, hostConfig);
            } catch (DockerException | InterruptedException e) {
                throw new ContainerException(e);
            }
            Map<String, StartedContainer> linkedContainers = new HashMap<>();
            for (Link link : links) {
                linkedContainers.put(link.alias, link.container);
            }
            startedContainer = new StartedContainer(client, container.id(), linkedContainers);
        }
        waitFor(blockUntil, System.currentTimeMillis() + secondsTimeout * 1000);
        return startedContainer;
    }

    private void waitFor(BlockUntil blockUntil, long timeToStop) {
        DockerContext context = new DockerContext(host, startedContainer);
        try {
            if (blockUntil.conditionMet(context)) {
                return;
            }
        } catch (Exception e) {
            //Exception is equivalent to false... carry on.
        }
        if (System.currentTimeMillis() > timeToStop) {
            String logMessage;
            try {
                logMessage = client.logs(id, DockerClient.LogsParameter.STDOUT, DockerClient.LogsParameter.STDERR).readFully();
            } catch (DockerException | InterruptedException e) {
                logMessage = "An error occurred trying to pull the logs from the docker container.";
            }
            for (StartedContainer linkedContainer : startedContainer.linkedContainers()) {
                linkedContainer.stop();
            }
            startedContainer.stop();
            throw new ContainerStartupTimeoutException(logMessage);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //Do we need to worry about this?
        }
        waitFor(blockUntil, timeToStop);
    }

    void stopIfStarted() {
        if (startedContainer != null) {
            startedContainer.stop();
        }
    }

    public static class Env {
        private final String key;
        private final String value;

        public Env(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class ContainerException extends RuntimeException {
        public ContainerException(Exception e) {
            super(e);
        }
    }

    public static class ContainerStartupTimeoutException extends RuntimeException {

        private final String logMessage;

        public ContainerStartupTimeoutException(String logMessage) {
            this.logMessage = logMessage;
        }

        public String containerLog() {
            return logMessage;
        }
    }

}
