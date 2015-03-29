package com.ebay.epd.dockerrunner;

import com.ebay.epd.dockerrunner.DockerHostFactory.DockerHost;
import com.github.dockerjava.api.DockerClient;
import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerRunner {
    private final DockerHost host;
    private final List<Container> containers = new ArrayList<>();

    public DockerRunner() {
        this(new DockerHostFactory());
    }

    DockerRunner(DockerHostFactory dockerHostFactory) {
        this.host = dockerHostFactory.dockerHostForEnvironment(System.getenv());
        ;
    }

    public ContainerBuilder containerFor(String image) {
        DockerHost dockerHost = new DockerHostFactory().dockerHostForEnvironment(System.getenv());
        return new ContainerBuilder(image, containers, dockerHost.client());
    }

    public String host() {
        return host.host();
    }

    public void stopAll() {
        for (Container container : containers) {
            container.stopIfStarted();
        }
    }

    static class VolumeMapping {
        final String hostPath;
        final String containerPath;

        VolumeMapping(String hostPath, String containerPath) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
        }
    }

    public class ContainerBuilder {
        private final String image;
        private final List<Container> containers;
        private final DockerClient client;
        private final Map<String, Container> linkedContainers = new HashMap<>();
        private Option<String> cpuset = Option.None();
        private Option<Memory> memory = Option.None();
        private List<Container.Env> envs = new ArrayList<>();
        private Option<String> dns = Option.None();
        private final List<VolumeMapping> volumeMappings = new ArrayList<>();
        private final List<Integer> additionalPorts = new ArrayList<>();

        ContainerBuilder(String image, List<Container> containers, DockerClient client) {
            this.image = image;
            this.containers = containers;
            this.client = client;
        }

        public LinkBuilder linkTo(Container container) {
            return new LinkBuilder(container);
        }

        public Container build() {
            Iterable<Link> links = Iterables.transform(linkedContainers.keySet(), new Function<String, Link>() {
                @Override
                public Link apply(String alias) {
                    return new Link(alias, linkedContainers.get(alias).start());
                }
            });
            Container container = new Container(client, image, links, host(), cpuset, memory, envs, dns, volumeMappings, additionalPorts);
            containers.add(container);
            return container;
        }

        public ContainerBuilder cpuset(String cpuset) {
            throw new UnsupportedOperationException();
//            this.cpuset = Option.Some(cpuset);
//            return this;
        }

        public ContainerBuilder memory(Memory memory) {
            throw new UnsupportedOperationException();
//            this.memory = Option.Some(memory);
//            return this;
        }

        public ContainerBuilder env(String key, String value) {
            envs.add(new Container.Env(key, value));
            return this;
        }

        public ContainerBuilder dns(String dns) {
            this.dns = Option.Some(dns);
            return this;
        }

        public VolumeBuilder mountHostVolume(String hostPath) {
            return new VolumeBuilder(hostPath);
        }

        public ContainerBuilder exposing(int additonalPort) {
            additionalPorts.add(additonalPort);
            return this;
        }

        public class LinkBuilder {

            private Container container;

            public LinkBuilder(Container container) {
                this.container = container;
            }

            public ContainerBuilder withAlias(String alias) {
                linkedContainers.put(alias, container);
                return ContainerBuilder.this;
            }
        }

        public class VolumeBuilder {
            private String hostPath;

            public VolumeBuilder(String hostPath) {
                this.hostPath = hostPath;
            }

            public ContainerBuilder toContainerVolume(String containerPath) {
                volumeMappings.add(new VolumeMapping(hostPath, containerPath));
                return ContainerBuilder.this;
            }
        }
    }

}
