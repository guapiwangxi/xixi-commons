package com.xi.commons.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 此类提供 waitForApplicationReady 方法，可以阻塞当前线程，直到 Spring 启动完成
 *
 * 警告：不要在 Spring 的启动线程中调用此方法，否则可能导致死锁
 */
@Component
public class SpringStartupUtils implements ApplicationListener<SpringApplicationEvent> {
    private static final CompletableFuture<ApplicationContext> future = new CompletableFuture<>();

    @Override
    public void onApplicationEvent(SpringApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            future.complete(((ApplicationReadyEvent) event).getApplicationContext());
        }
        if (event instanceof ApplicationFailedEvent) {
            future.completeExceptionally(((ApplicationFailedEvent) event).getException());
        }
    }

    @SneakyThrows
    public static void waitForApplicationReady() {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @SneakyThrows
    public static void waitForApplicationReady(long timeout, TimeUnit unit) {
        try {
            future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
