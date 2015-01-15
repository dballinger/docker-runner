package com.ebay.epd.dockerrunner;

public interface BlockUntil {
    boolean conditionMet(String host, StartedContainer container);
}
