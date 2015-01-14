package com.ebay.epd.dockerrunner;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.ebay.epd.dockerrunner.MemoryUnits.megabytes;
import static org.mockito.Mockito.*;

public class ContainerBuilderTest {

    private DockerClient spyClient;
    private DockerRunner dockerRunner;

    @Before
    public void before() {
        DockerHostFactory spyHostFactory = spy(new DockerHostFactory());
        spyClient = spy(new DockerHostFactory().dockerHostForEnvironment(System.getenv()).client());
        DockerHostFactory.DockerHost dockerHost = new DockerHostFactory.DockerHost() {
            @Override
            public DockerClient client() {
                return spyClient;
            }

            @Override
            public String host() {
                return "localhost";
            }
        };
        doReturn(dockerHost).when(spyHostFactory).dockerHostForEnvironment(any(Map.class));
        dockerRunner = new DockerRunner(spyHostFactory);
    }

    @Test
    public void shouldSpecifyCpusets() throws Exception {
        String cpuset = "0,1";
        dockerRunner.containerFor("spartans/docker-runner-image1").cpuset(cpuset).build().start().stop();
        verify(spyClient).createContainer(argThat(hasCpuset(cpuset)));
    }

    @Test
    public void shouldSpecifyMemory() throws Exception {
        long oneMeg = 104857600;
        dockerRunner.containerFor("spartans/docker-runner-image1").memory(megabytes(100)).build().start().stop();
        verify(spyClient).createContainer(argThat(hasMemory(oneMeg)));
    }

    private Matcher<ContainerConfig> hasCpuset(final String cpuset) {
        return new TypeSafeMatcher<ContainerConfig>() {
            @Override
            protected boolean matchesSafely(ContainerConfig config) {
                return cpuset.equals(config.cpuset());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ContainerConfig with cpuset: ").appendValue(cpuset);
            }
        };
    }

    private Matcher<ContainerConfig> hasMemory(final long bytes) {
        return new TypeSafeMatcher<ContainerConfig>() {
            @Override
            protected boolean matchesSafely(ContainerConfig config) {
                return bytes == config.memory();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ContainerConfig with memory: ").appendValue(bytes);
            }
        };
    }
}
