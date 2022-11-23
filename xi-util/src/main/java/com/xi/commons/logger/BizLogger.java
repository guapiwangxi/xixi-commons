package com.xi.commons.logger;

import org.springframework.boot.logging.LogLevel;

import java.util.Map;

/**
 * 业务日志打印类，在文件中打印出 `key1=value1`key2=value2 的格式，方便在 xflush 中配置监控
 */
public interface BizLogger {

    /**
     * 往日志中添加 key 和 value，返回 this 方便链式调用
     */
    BizLogger ap(String key, Object value);

    /**
     * 往日志中添加 key 和 value，返回 this 方便链式调用
     */
    BizLogger ap(Map<String, Object> values);

    /**
     * 打印该行日志，日志级别为 INFO
     */
    void println();

    /**
     * 打印该行日志，使用指定的日志级别
     */
    void println(LogLevel logLevel);
}
