package com.xi.commons.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 异步工具类，使用一个公共线程池处理异步任务
 */
public class AsyncUtils {
    private static final ExecutorService commonThreadPool = createCommonThreadPool();

    private static ExecutorService createCommonThreadPool() {
        ExecutorService threadPool = AsyncThreadPool.newFixedThreadPool(64, new NamedThreadFactory("Async-", true));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                threadPool.shutdown();

                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                // ignored...
            }
        }));

        return threadPool;
    }

    /**
     * 获取异步线程池
     */
    public static ExecutorService getCommonThreadPool() {
        return commonThreadPool;
    }

    /**
     * 提交异步任务
     */
    public static void execute(Runnable task) {
        commonThreadPool.execute(task);
    }

    /**
     * 提交异步任务，返回 Future
     */
    public static Future<?> submit(Runnable task) {
        return commonThreadPool.submit(task);
    }

    /**
     * 提交异步任务，返回 Future
     */
    public static <T> Future<T> submit(Runnable task, T result) {
        return commonThreadPool.submit(task, result);
    }

    /**
     * 提交异步任务，返回 Future
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return commonThreadPool.submit(task);
    }
}
