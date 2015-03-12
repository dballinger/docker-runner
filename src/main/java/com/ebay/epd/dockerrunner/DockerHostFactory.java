package com.ebay.epd.dockerrunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class DockerHostFactory {

    public DockerHost dockerHostForEnvironment(Map<String, String> env) {
        try {
            String hostKey = "DOCKER_HOST";
            String tlsVerifyKey = "DOCKER_TLS_VERIFY";
            String certPathKey = "DOCKER_CERT_PATH";
            if ("1".equals(env.get(tlsVerifyKey))) {
                return new BootToDockerTlsHost(env.get(hostKey), env.get(certPathKey));
            } else if (env.containsKey(hostKey)) {
                return new BootToDockerHost(env.get(hostKey));
            } else {
                return new NativeDockerHost();
            }
        } catch (URISyntaxException e) {
            throw new DockerHostException(e);
        }
    }

    public static interface DockerHost {
        String host();
    }

    public static class BootToDockerHost implements DockerHost {

        private final String host;

        public BootToDockerHost(String dockerHost) throws URISyntaxException {
            URI tcpUri = new URI(dockerHost);
            host = tcpUri.getHost();
            URI httpUri = new URI("http", null, host, tcpUri.getPort(), null, null, null);
        }


        @Override
        public String host() {
            return host;
        }
    }

    public static class NativeDockerHost implements DockerHost {


        @Override
        public String host() {
            return "localhost";
        }
    }

    public static class BootToDockerTlsHost implements DockerHost {

        private final String host;

        public BootToDockerTlsHost(String dockerHost, String certPath) throws URISyntaxException {
            URI tcpUri = new URI(dockerHost);
            host = tcpUri.getHost();
            URI httpUri = new URI("https", null, host, tcpUri.getPort(), null, null, null);
//            try {
//                dockerClient = DefaultDockerClient.builder()
//                                .uri(httpUri)
//                                .dockerCertificates(new DockerCertificates(new File(certPath).toPath()))
//                                .build();
//            } catch (DockerCertificateException e) {
//                throw new DockerHostCertificateException(e);
//            }
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

    public static class DockerHostCertificateException extends RuntimeException {
        public DockerHostCertificateException(Throwable cause) {
            super("Problem with the docker host cert path", cause);
        }
    }
}
