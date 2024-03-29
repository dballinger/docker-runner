package com.ebay.epd.dockerrunner;

public class MemoryUnits {
    public static Memory megabytes(final int meg) {
        return new Memory() {
            @Override
            public long toBytes() {
                return meg * 1024 * 1024;
            }
        };
    }
    public static Memory gigabytes(final int gig) {
        return new Memory() {
            @Override
            public long toBytes() {
                return gig * 1024 * 1024 * 1024;
            }
        };
    }
}
