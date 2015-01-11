package com.ebay.epd.dockerrunner;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class DockerHostFactory {

    public static DockerHost dockerHostForEnvironment(Map<String, String> env) {
        try {
            return new BootToDockerHost(env.get("DOCKER_HOST"));
        } catch (URISyntaxException e) {
            throw new DockerHostException(e);
        }
    }

    public static interface DockerHost {
        DockerClient client();
        String host();
    }

    public static class BootToDockerHost implements DockerHost {

        private final DefaultDockerClient dockerClient;
        private final String host;

        public BootToDockerHost(String dockerHost) throws URISyntaxException {
            URI tcpUri = new URI(dockerHost);
            host = tcpUri.getHost();
            URI httpUri = new URI("http", null, host, tcpUri.getPort(), null, null, null);
            dockerClient = new DefaultDockerClient(httpUri);
        }

        @Override
        public DockerClient client() {
            return dockerClient;
        }

        @Override
        public String host() {
            return host;
        }
    }

    public static class DockerHostException extends RuntimeException {
        public DockerHostException(Throwable cause) {
            super("DOCKER_HOST env is an invalid url", cause);
        }
    }
}
