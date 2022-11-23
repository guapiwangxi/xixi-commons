package com.xi.commons.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by vince at Apr 06, 2021.
 */
public class BizLoggerImpl implements BizLogger {
    private final String loggerName;
    private final Map<String, Object> values;
    private final boolean collectNestedLogs;

    public BizLoggerImpl(String loggerName) {
        this(loggerName, false);
    }

    BizLoggerImpl(String loggerName, boolean collectNestedLogs) {
        this.loggerName = loggerName;
        this.values = new LinkedHashMap<>();
        this.collectNestedLogs = collectNestedLogs;
    }

    @Override
    public BizLogger ap(String key, Object value) {
        this.values.put(key, value);
        return this;
    }

    @Override
    public BizLogger ap(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }

    @Override
    public void println() {
        println(LogLevel.INFO);
    }

    @Override
    public void println(LogLevel logLevel) {
        Logger logger = LoggerFactory.getLogger(loggerName);

        if (logLevel == LogLevel.TRACE && logger.isTraceEnabled()) {
            logger.trace(generateLogContent());
        }

        if (logLevel == LogLevel.DEBUG && logger.isDebugEnabled()) {
            logger.debug(generateLogContent());
        }

        if (logLevel == LogLevel.INFO && logger.isInfoEnabled()) {
            logger.info(generateLogContent());
        }

        if (logLevel == LogLevel.WARN && logger.isWarnEnabled()) {
            logger.warn(generateLogContent());
        }

        if (logLevel == LogLevel.ERROR && logger.isErrorEnabled()) {
            logger.error(generateLogContent());
        }
    }

    private String generateLogContent() {
        StringBuilder sb = new StringBuilder();

        values.forEach((key, value) -> {
            if (key != null && value != null) {
                sb.append("`").append(key).append("=").append(value);
            }
        });

        return sb.toString();
    }

    public String getLoggerName() {
        return loggerName;
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    boolean isCollectNestedLogs() {
        return collectNestedLogs;
    }
}
