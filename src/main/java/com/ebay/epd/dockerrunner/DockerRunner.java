package com.ebay.epd.dockerrunner;

import com.ebay.epd.dockerrunner.DockerHostFactory.DockerHost;

import java.util.ArrayList;
import java.util.List;

public class DockerRunner {
    private DockerHost host = DockerHostFactory.dockerHostForEnvironment(System.getenv());
    private List<Container> containers = new ArrayList<>();

    public Container newContainer(String image) {
        Container container = new Container(host.client(), image);
        containers.add(container);
        return container;
    }

    public String host() {
        return host.host();
    }

    public void stopAll() {
        for (Container container : containers) {
            container.stopIfStarted();
        }
    }
}
