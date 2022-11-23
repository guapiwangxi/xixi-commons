package com.xi.commons.async;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by vince at Aug 16, 2021.
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger counter = new AtomicInteger();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ThreadGroup group;
    private final String prefix;
    private final boolean daemon;

    public NamedThreadFactory(String prefix) {
        this.group = Thread.currentThread().getThreadGroup();
        this.prefix = prefix;
        this.daemon = false;
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        this.group = Thread.currentThread().getThreadGroup();
        this.prefix = prefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(group, r);
        thread.setName(prefix + counter.incrementAndGet());
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler((t, e) -> logger.error("Uncaught error: ", e));
        return thread;
    }
}
