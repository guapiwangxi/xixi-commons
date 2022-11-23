package com.xi.commons.aspect.advice;

import org.springframework.boot.logging.LogLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 把此注解标注在方法上，会在此方法内部开启一个 ZLog 上下文
 *
 * 进入方法时，调用 ZLog.start 方法，退出方法时，调用 ZLog.end 方法打印日志。同时，此注解还会自动打印方法名，traceId 等通用数据
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseZLog {

    /**
     * 日志名
     */
    String logger();

    /**
     * 日志打印级别
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 是否收集嵌套的日志
     */
    boolean collectNestedLogs() default false;
}
