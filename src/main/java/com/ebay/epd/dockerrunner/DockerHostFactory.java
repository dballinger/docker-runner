package com.ebay.epd.dockerrunner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class DockerHostFactory {

    public DockerHost dockerHostForEnvironment(Map<String, String> env) {
        try {
            String hostKey = "DOCKER_HOST";
            if (env.containsKey(hostKey)) {
                return new BootToDockerTlsHost(env.get(hostKey));
            } else {
                return new NativeDockerHost();
            }
        } catch (URISyntaxException e) {
            throw new DockerHostException(e);
        }
    }

    public static interface DockerHost {
        String host();

        DockerClient client();
    }

    public static class NativeDockerHost implements DockerHost {

        @Override
        public String host() {
            return "localhost";
        }

        @Override
        public DockerClient client() {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                                         .withUri("unix:///var/run/docker.sock")
                                         .build();
            return DockerClientBuilder.getInstance(config).build();
        }
    }

    public static class BootToDockerTlsHost implements DockerHost {

        private final String host;

        public BootToDockerTlsHost(String dockerHost) throws URISyntaxException {
            URI tcpUri = new URI(dockerHost);
            host = tcpUri.getHost();
        }


        @Override
        public String host() {
            return host;
        }

        @Override
        public DockerClient client() {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().build();
            return DockerClientBuilder.getInstance(config).build();
        }
    }

    public static class DockerHostException extends RuntimeException {
        public DockerHostException(Throwable cause) {
            super("DOCKER_HOST env is an invalid url", cause);
        }
    }

}
