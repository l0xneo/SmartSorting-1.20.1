package com.smartsorting.routing;

import java.util.concurrent.atomic.AtomicLong;

public final class NetworkVersion {

    private static final AtomicLong VERSION = new AtomicLong();

    private NetworkVersion() {
    }

    public static long get() {
        return VERSION.get();
    }

    public static void bump() {
        VERSION.incrementAndGet();
    }
}
