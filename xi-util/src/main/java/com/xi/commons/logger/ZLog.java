package com.xi.commons.logger;

import org.springframework.boot.logging.LogLevel;

import java.util.*;

/**
 * Created by vince at Apr 06, 2021.
 */
public final class ZLog {
    private static final ThreadLocal<Deque<BizLoggerImpl>> logStacks = new ThreadLocal<>();
    private static final BizLogger delegatingBizLogger = new DelegatingBizLogger();

    /**
     * 在当前线程开启 ZLog 上下文日志，每个 start 调用一定要有相应的 end 调用，支持嵌套
     */
    public static void start(String loggerName) {
        start(loggerName, false);
    }

    /**
     * 在当前线程开启 ZLog 上下文日志，每个 start 调用一定要有相应的 end 调用，支持嵌套
     *
     * collectNestedLogs 可配置是否收集内部嵌套的日志数据
     */
    public static void start(String loggerName, boolean collectNestedLogs) {
        Deque<BizLoggerImpl> stack = logStacks.get();
        if (stack == null) {
            // 初始化 ZLog 上下文栈
            stack = new LinkedList<>();
            logStacks.set(stack);
        }

        // 入栈
        stack.push(new BizLoggerImpl(loggerName, collectNestedLogs));
    }

    /**
     * 结束当前线程最近一个开启的 ZLog 上下文日志，并输出日志内容到文件，使用 INFO 日志级别
     */
    public static void end() {
        end(LogLevel.INFO);
    }

    /**
     * 结束当前线程最近一个开启的 ZLog 上下文日志，并输出日志内容到文件，使用指定的日志级别
     */
    public static void end(LogLevel logLevel) {
        Deque<BizLoggerImpl> stack = logStacks.get();
        if (stack == null) {
            throw new IllegalStateException("There is no ZLog context in the current thread.");
        }

        try {
            // 出栈，并打印日志
            stack.pop().println(logLevel);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("There is no ZLog context in the current thread.", e);
        }

        // 如果栈为空，清除 thread local 变量，防止内存泄露
        if (stack.isEmpty()) {
            logStacks.remove();
        }
    }

    /**
     * 往 ZLog 上下文日志中添加 key 和 value，返回一个代理的 BizLogger 对象方便链式调用
     */
    public static BizLogger ap(String key, Object value) {
        Deque<BizLoggerImpl> stack = logStacks.get();
        if (stack == null) {
            return delegatingBizLogger;
        }

        Iterator<BizLoggerImpl> iterator = stack.iterator();

        // 第一个是当前上下文，不需要判断，直接添加
        if (iterator.hasNext()) {
            iterator.next().ap(key, value);
        }

        // 对于嵌套的父级上下文，如果开启了 collectNestedLogs，则把数据往上传播
        while (iterator.hasNext()) {
            BizLoggerImpl logger = iterator.next();
            if (logger.isCollectNestedLogs()) {
                logger.ap(key, value);
            }
        }

        return delegatingBizLogger;
    }

    /**
     * 往 ZLog 上下文日志中添加 key 和 value，返回一个代理的 BizLogger 对象方便链式调用
     */
    public static BizLogger ap(Map<String, Object> values) {
        Deque<BizLoggerImpl> stack = logStacks.get();
        if (stack == null) {
            return delegatingBizLogger;
        }

        Iterator<BizLoggerImpl> iterator = stack.iterator();

        // 第一个是当前上下文，不需要判断，直接添加
        if (iterator.hasNext()) {
            iterator.next().ap(values);
        }

        // 对于嵌套的父级上下文，如果开启了 collectNestedLogs，则把数据往上传播
        while (iterator.hasNext()) {
            BizLoggerImpl logger = iterator.next();
            if (logger.isCollectNestedLogs()) {
                logger.ap(values);
            }
        }

        return delegatingBizLogger;
    }

    /**
     * 代理对象，使链式调用的所有 ap 函数，都使用 ZLog.ap，以支持上下文嵌套的逻辑
     */
    private static class DelegatingBizLogger implements BizLogger {
        @Override
        public BizLogger ap(String key, Object value) {
            return ZLog.ap(key, value);
        }

        @Override
        public BizLogger ap(Map<String, Object> values) {
            return ZLog.ap(values);
        }

        @Override
        public void println() {
            throw new UnsupportedOperationException("println is not supported.");
        }

        @Override
        public void println(LogLevel logLevel) {
            throw new UnsupportedOperationException("println is not supported.");
        }
    }
}
