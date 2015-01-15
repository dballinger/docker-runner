package com.ebay.epd.dockerrunner;

class Link {
    final String alias;
    final StartedContainer container;

    Link(String alias, StartedContainer container) {
        this.container = container;
        this.alias = alias;
    }
}
