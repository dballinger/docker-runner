package com.ebay.epd.dockerrunner;

import com.ebay.epd.dockerrunner.DockerHostFactory.DockerHost;

public class DockerRunner {
    private DockerHost host = DockerHostFactory.dockerHostForEnvironment(System.getenv());

    public Container newContainer(String image) {
        return new Container(host.client(), image);
    }

    public String host() {
        return host.host();
    }
}
