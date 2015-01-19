package com.ebay.epd.dockerrunner;

public interface BlockUntil {
    boolean conditionMet(DockerContext context);
}
