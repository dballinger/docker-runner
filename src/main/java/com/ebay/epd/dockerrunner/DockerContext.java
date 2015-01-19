package com.ebay.epd.dockerrunner;

public class DockerContext {
    private final String host;
    private final StartedContainer container;

    public DockerContext(String host, StartedContainer container) {
        this.host = host;
        this.container = container;
    }

    public String host() {
        return host;
    }

    public StartedContainer container() {
        return container;
    }
}
