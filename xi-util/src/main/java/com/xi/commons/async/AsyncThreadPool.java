package com.xi.commons.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.global.satellite.proxy.concurrent.SatelliteRunnableWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * 异步线程池，支持透传 EagleEye 上下文，防止因为异步丢失压测标等依赖于 ThreadLocal 的数据
 *
 * 注意，为保证压测标能全链路透传，应确保应用中所有业务线程池都是此类的示例，禁止使用 JDK 自带的 Executors 工具类自行创建线程池，也禁止自行
 * 手动创建 ThreadPoolExecutor 实例，应改用 AsyncThreadPool 代替
 */
public class AsyncThreadPool extends ThreadPoolExecutor {

    public AsyncThreadPool(int corePoolSize,
                           int maximumPoolSize,
                           long keepAliveTime,
                           TimeUnit unit,
                           BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public AsyncThreadPool(int corePoolSize,
                           int maximumPoolSize,
                           long keepAliveTime,
                           TimeUnit unit,
                           BlockingQueue<Runnable> workQueue,
                           ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public AsyncThreadPool(int corePoolSize,
                           int maximumPoolSize,
                           long keepAliveTime,
                           TimeUnit unit,
                           BlockingQueue<Runnable> workQueue,
                           RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public AsyncThreadPool(int corePoolSize,
                           int maximumPoolSize,
                           long keepAliveTime,
                           TimeUnit unit,
                           BlockingQueue<Runnable> workQueue,
                           ThreadFactory threadFactory,
                           RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        super.execute(SatelliteRunnableWrapper.of(command));
    }

    public static AsyncThreadPool newFixedThreadPool(int nThreads) {
        return new AsyncThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    }

    public static AsyncThreadPool newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new AsyncThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), threadFactory);
    }

    public static AsyncThreadPool newSingleThreadExecutor() {
        return new AsyncThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
    }

    public static AsyncThreadPool newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new AsyncThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), threadFactory);
    }

    public static AsyncThreadPool newCachedThreadPool() {
        return new AsyncThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    public static AsyncThreadPool newCachedThreadPool(ThreadFactory threadFactory) {
        return new AsyncThreadPool(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
    }
}
