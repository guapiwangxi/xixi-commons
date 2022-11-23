package com.xi.commons.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 启用 Spring 异步任务，支持 @Async 注解，所有异步任务默认在 AsyncUtils.getCommonThreadPool() 公共线程池上执行
 */
@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean("asyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        return AsyncUtils.getCommonThreadPool();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            String methodName = method.getDeclaringClass().getName() + "." + method.getName();
            logger.error(methodName, ex);
        };
    }
}
