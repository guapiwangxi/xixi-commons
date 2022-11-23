package com.xi.commons.logger;

import java.nio.charset.Charset;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * 日志自动配置服务
 * 
 * @author qian.lqlq
 * @version $Id: LogConfiguration.java, v 0.1 2019年08月07日 13:56 qian.lqlq Exp $
 */
@Slf4j
@Configuration
public class BizLogConfiguration implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private BizLogProperties logProperty;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info(String.format("Logger properties: %s", logProperty.toString()));

        List<String> loggers = logProperty.getLoggers();
        if (loggers == null) {
            loggers = Lists.newArrayList();
        }

        // 默认配置 facade.log 和 integration.log
        loggers.add(String.format("%s", logProperty.getFacadeLoggerName()));
        loggers.add(String.format("%s", logProperty.getIntegrationLoggerName()));

        // 初始化业务日志配置
        for (String n : loggers) {
            String[] names = StringUtils.splitPreserveAllTokens(n, ",");
            if (names.length != 1 && names.length != 2) {
                continue;
            }

            String loggerName = names[0];
            String errorLoggerName = names.length == 2 ? names[1] : loggerName + "-error";

            Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
            logger.setLevel(Level.valueOf(logProperty.getLevel().toUpperCase()));
            logger.setAdditive(false);
            logger.addAppender(buildAppender(loggerName, Level.INFO));
            logger.addAppender(buildAppender(errorLoggerName, Level.ERROR));
        }
    }

    private FileAppender<ILoggingEvent> buildAppender(String name, Level level) {
        Context context = (Context) LoggerFactory.getILoggerFactory();

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName(name);
        appender.setFile(OptionHelper.substVars(String.format("%s/%s.log", logProperty.getFilePath(), name), context));
        appender.setAppend(true);
        appender.setPrudent(false);
        appender.addFilter(buildFilter(level));
        appender.setEncoder(buildEncoder(context));
        appender.setRollingPolicy(buildRollingPolicy(appender));
        appender.start();
        return appender;
    }

    private Filter<ILoggingEvent> buildFilter(Level level) {
        LevelFilter filter = new LevelFilter();
        if (level == Level.ERROR) {
            filter.setLevel(level);
            filter.setOnMatch(FilterReply.ACCEPT);
            filter.setOnMismatch(FilterReply.DENY);
        } else {
            filter.setLevel(Level.ERROR);
            filter.setOnMatch(FilterReply.DENY);
        }

        filter.start();
        return filter;
    }

    private Encoder<ILoggingEvent> buildEncoder(Context context) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setCharset(Charset.forName(logProperty.getEncodeCharset()));
        encoder.setPattern(logProperty.getEncodePattern());
        encoder.start();
        return encoder;
    }

    private RollingPolicy buildRollingPolicy(FileAppender<ILoggingEvent> appender) {
        String fileNamePattern = OptionHelper.substVars(
            String.format("%s/%s.log", logProperty.getFilePath(), appender.getName()) + ".%d{yyyy-MM-dd}.%i",
            appender.getContext()
        );

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
        policy.setFileNamePattern(fileNamePattern);
        policy.setMaxFileSize(FileSize.valueOf(logProperty.getMaxFileSize()));
        policy.setMaxHistory(logProperty.getMaxHistory());
        policy.setTotalSizeCap(FileSize.valueOf(logProperty.getTotalFileSize()));
        policy.setParent(appender);
        policy.setContext(appender.getContext());
        policy.start();
        return policy;
    }
}