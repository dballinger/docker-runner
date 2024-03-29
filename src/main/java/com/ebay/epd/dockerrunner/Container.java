package com.ebay.epd.dockerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.*;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Container {
    private final DockerClient client;
    private final String imageName;
    private final Iterable<Link> links;
    private final String host;
    private Option<String> cpuset;
    private Option<Memory> memory;
    private List<Env> envs;
    private StartedContainer startedContainer;
    private Option<String> dns;
    private List<DockerRunner.VolumeMapping> volumeMappings;
    private List<Integer> additionalPorts;
    private final BlockUntil noBlock = new BlockUntil() {
        @Override
        public boolean conditionMet(DockerContext context) {
            return true;
        }
    };
    private String id;

    public Container(DockerClient client, String imageName, Iterable<Link> links, String host, Option<String> cpuset, Option<Memory> memory, List<Env> envs, Option<String> dns, List<DockerRunner.VolumeMapping> volumeMappings, List<Integer> additionalPorts) {
        this.client = client;
        this.imageName = imageName;
        this.links = links;
        this.host = host;
        this.cpuset = cpuset;
        this.memory = memory;
        this.envs = envs;
        this.dns = dns;
        this.volumeMappings = volumeMappings;
        this.additionalPorts = additionalPorts;
    }

    public StartedContainer start() {
        return start(noBlock, 0);
    }

    public StartedContainer start(BlockUntil blockUntil, int secondsTimeout) {
        if (startedContainer == null) {
            pullIfRequired();
            List<String> envStrs = Lists.transform(envs, new Function<Env, String>() {
                @Override
                public String apply(Env input) {
                    return String.format("%s=%s", input.key, input.value);
                }
            });
            List<Bind> volumes = Lists.transform(volumeMappings, new Function<DockerRunner.VolumeMapping, Bind>() {
                @Override
                public Bind apply(DockerRunner.VolumeMapping input) {
                    return new Bind(input.hostPath, new Volume(input.containerPath));
                }
            });
            List<ExposedPort> exposePorts = Lists.transform(additionalPorts, new Function<Integer, ExposedPort>() {
                @Override
                public ExposedPort apply(Integer input) {
                    return new ExposedPort(input);
                }
            });
            id = client
                  .createContainerCmd(imageName)
                  .withEnv(envStrs.toArray(new String[]{}))
                  .withExposedPorts(exposePorts.toArray(new ExposedPort[]{}))
                  .exec()
                  .getId();
            final StartContainerCmd startContainerCmd = client
                                                         .startContainerCmd(id).withBinds()
                                                         .withPublishAllPorts(true)
                                                         .withLinks(containerLinks())
                                                         .withBinds(volumes.toArray(new Bind[]{}));

            dns.doIfPresent(new Option.OptionalCommand<String>() {
                @Override
                public void apply(String dns) {
                    startContainerCmd.withDns(dns);
                }
            });
            try {
                UUID correlationId = UUID.randomUUID();
                System.out.println(String.format("DockerRunner %s, starting container for image %s at time %s", correlationId, imageName, System.currentTimeMillis()));
                startContainerCmd.exec();
                InspectContainerResponse inspectContainerResponse = client.inspectContainerCmd(id).exec();
                for (Ports.Binding[] bindings : inspectContainerResponse.getNetworkSettings().getPorts().getBindings().values()) {
                    for (Ports.Binding binding : bindings) {
                        System.out.println(String.format("DockerRunner %s, started container for image %s at time %s with port binding %s", correlationId, imageName, System.currentTimeMillis(), binding.getHostPort()));
                    }
                }
            } catch (NotModifiedException e) {
                //swallow... this is fine!
            }
            Map<String, StartedContainer> linkedContainers = new HashMap<>();
            for (Link link : links) {
                linkedContainers.put(link.alias, link.container);
            }
            startedContainer = new StartedContainer(client, id, linkedContainers);
        }
        waitFor(blockUntil, System.currentTimeMillis() + secondsTimeout * 1000);
        return startedContainer;
    }

    private com.github.dockerjava.api.model.Link[] containerLinks() {
        Iterable<com.github.dockerjava.api.model.Link> contLinkIterable = Iterables.transform(links, new Function<Link, com.github.dockerjava.api.model.Link>() {
            @Override
            public com.github.dockerjava.api.model.Link apply(Link input) {
                return new com.github.dockerjava.api.model.Link(input.container.name(), input.alias);
            }
        });

        int linkCount = Iterables.size(contLinkIterable);
        com.github.dockerjava.api.model.Link[] contLinks = new com.github.dockerjava.api.model.Link[linkCount];
        int i = 0;
        for (com.github.dockerjava.api.model.Link link : contLinkIterable) {
            contLinks[i++] = link;
        }
        return contLinks;
    }

    private void pullIfRequired() {
        String nameWithTag;
        if (imageName.contains(":")) {
            nameWithTag = imageName;
        } else {
            nameWithTag = imageName + ":latest";
        }
        List<Image> images = client.listImagesCmd().exec();
        for (Image image : images) {
            for (String name : image.getRepoTags()) {
                if (name.equals(nameWithTag)) {
                    return;
                }
            }
        }
        InputStream pullLog = client.pullImageCmd(imageName).exec();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pullLog));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitFor(BlockUntil blockUntil, long timeToStop) {
        DockerContext context = new DockerContext(host, startedContainer);
        while (true) {
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
                    logMessage = IOUtils.toString(client.logContainerCmd(id).withStdErr().withStdOut().exec());
                } catch (IOException e) {
                    logMessage = "An error occurred trying to pull the logs from the docker container.";
                }
                for (StartedContainer linkedContainer : startedContainer.linkedContainers()) {
                    linkedContainer.stop();
                }
                startedContainer.stop();
                System.out.println(logMessage);
                throw new ContainerStartupTimeoutException(logMessage);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //Do we need to worry about this?
            }
        }
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
