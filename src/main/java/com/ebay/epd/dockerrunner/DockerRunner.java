package com.ebay.epd.dockerrunner;

import com.ebay.epd.dockerrunner.DockerHostFactory.DockerHost;
import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerRunner {
    private DockerHost host = DockerHostFactory.dockerHostForEnvironment(System.getenv());
    private List<Container> containers = new ArrayList<>();

    public ContainerBuilder containerFor(String image) {
        return new ContainerBuilder(image);
    }

    public String host() {
        return host.host();
    }

    public void stopAll() {
        for (Container container : containers) {
            container.stopIfStarted();
        }
    }

    public class ContainerBuilder {
        private String image;
        private Map<String, Container> linkedContainers = new HashMap<>();

        public ContainerBuilder(String image) {
            this.image = image;
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
            Container container = new Container(host.client(), image, links, host());
            containers.add(container);
            return container;
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
