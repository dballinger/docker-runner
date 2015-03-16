package com.ebay.epd.dockerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
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
    private Option<String> dns;
    private final BlockUntil noBlock = new BlockUntil() {
        @Override
        public boolean conditionMet(DockerContext context) {
            return true;
        }
    };
    private String id;

    public Container(DockerClient client, String imageName, Iterable<Link> links, String host, Option<String> cpuset, Option<Memory> memory, List<Env> envs, Option<String> dns) {
        this.client = client;
        this.imageName = imageName;
        this.links = links;
        this.host = host;
        this.cpuset = cpuset;
        this.memory = memory;
        this.envs = envs;
        this.dns = dns;
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
            id = client
                    .createContainerCmd(imageName)
                    .withEnv(envStrs.toArray(new String[]{}))
                    .exec()
                    .getId();
            final StartContainerCmd startContainerCmd = client
                    .startContainerCmd(id)
                    .withPublishAllPorts(true)
                    .withLinks(containerLinks());

            dns.doIfPresent(new Option.OptionalCommand<String>() {
                @Override
                public void apply(String dns) {
                    startContainerCmd.withDns(dns);
                }
            });
            startContainerCmd.exec();
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
            while((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitFor(BlockUntil blockUntil, long timeToStop) {
        DockerContext context = new DockerContext(host, startedContainer);
        while(true) {
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
            try {
                startedContainer.stop();
            } catch(NotModifiedException e) {
                //swallow
                e.printStackTrace();
            }
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
