package com.ebay.epd.dockerrunner;

import com.ebay.epd.dockerrunner.DockerHostFactory.DockerHost;
import com.spotify.docker.client.DockerClient;
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
        this.host = dockerHostFactory.dockerHostForEnvironment(System.getenv());;
    }

    public ContainerBuilder containerFor(String image) {
        return new ContainerBuilder(host.client(), image, containers);
    }

    public String host() {
        return host.host();
    }

    public void stopAll() {
        for (Container container : containers) {
            container.stopIfStarted();
        }
    }

    class ContainerBuilder {
        private final DockerClient client;
        private final String image;
        private final List<Container> containers;
        private final Map<String, Container> linkedContainers = new HashMap<>();
        private Option<String> cpuset = Option.None();
        private Option<Memory> memory = Option.None();

        ContainerBuilder(DockerClient client, String image, List<Container> containers) {
            this.client = client;
            this.image = image;
            this.containers = containers;
        }

        public LinkBuilder linkTo(Container container) {
            return new LinkBuilder(container);
        }

        public Container build() {
            Iterable<Link> links = Iterables.transform(linkedContainers.keySet(), new Function<String, Link>() {
                @Override
                public Link apply(String alias) {
                    String name = linkedContainers.get(alias).start().name();
                    return new Link(name, alias);
                }
            });
            Container container = new Container(client, image, links, host(), cpuset, memory);
            containers.add(container);
            return container;
        }

        public ContainerBuilder cpuset(String cpuset) {
            this.cpuset = Option.Some(cpuset);
            return this;
        }

        public ContainerBuilder memory(Memory memory) {
            this.memory = Option.Some(memory);
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
    }

}
