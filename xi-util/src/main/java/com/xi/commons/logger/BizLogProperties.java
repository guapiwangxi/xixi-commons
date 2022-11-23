package com.xi.commons.logger;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * @author qian.lqlq
 * @version $Id: LogProperty.java, v 0.1 2019年08月07日 15:44 qian.lqlq Exp $
 */
@Data
@Component
public class BizLogProperties {

    /**
     * 日志编码字符集，默认为UTF-8
     */
    @Value("${zal.commons.logger.encodeCharset:UTF-8}")
    private String       encodeCharset;

    /**
     * 日志头部格式
     */
    @Value("${zal.commons.logger.encodePattern:%d{yyyy-MM-dd HH:mm:ss.SSS}|\"|%thread|\"|%level|\"|%logger|\"|%msg%n}")
    private String       encodePattern;

    /**
     * 单文件最大容量，默认为50M
     */
    @Value("${zal.commons.logger.maxFileSize:50MB}")
    private String       maxFileSize;

    /**
     * 文件总容量，默认为20G
     */
    @Value("${zal.commons.logger.totalFileSize:20GB}")
    private String       totalFileSize;

    /**
     * 日志保留最长时间，默认为7天
     */
    @Value("${zal.commons.logger.maxHistory:7}")
    private Integer      maxHistory;

    /**
     * 日志文件路径，默认为/userHome/logs/${appName}，例如/home/admin/logs/zal-social-share
     */
    @Value("${zal.commons.logger.filePath:${user.home}/logs/${spring.application.name}}")
    private String       filePath;

    /**
     * 日志级别，默认为info
     */
    @Value("${zal.commons.logger.level:info}")
    private String       level;

    /**
     * appender，格式为normalAppenderA,errorAppenderB;normalAppenderB
     */
    @Value("#{'${zal.commons.logger.loggers:}'.split(';')}")
    private List<String> loggers;

    /**
     * facade拦截器日志名字
     */
    @Value("${zal.commons.logger.facadeLoggerName:facade}")
    private String       facadeLoggerName;

    /**
     * integration拦截器日志名字
     */
    @Value("${zal.commons.logger.integrationLoggerName:integration}")
    private String       integrationLoggerName;

    @Override
    public String toString() {
        return "LogProperty{" + "encodeCharset='" + encodeCharset + '\'' + ", encodePattern='"
               + encodePattern + '\'' + ", maxFileSize='" + maxFileSize + '\'' + ", totalFileSize='"
               + totalFileSize + '\'' + ", maxHistory=" + maxHistory + ", filePath='" + filePath
               + '\'' + ", level='" + level + '\'' + ", loggers=" + loggers + ", facadeLoggerName='"
               + facadeLoggerName + '\'' + ", integrationLoggerName='" + integrationLoggerName
               + '\'' + '}';
    }
}