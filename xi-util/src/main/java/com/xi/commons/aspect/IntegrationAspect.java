package com.xi.commons.aspect;

import com.xi.commons.aspect.advice.Integration;
import com.xi.commons.logger.BizLogProperties;
import com.xi.commons.util.LocalizationUtils;
import com.xi.commons.util.ZalException;
import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import com.taobao.eagleeye.EagleEye;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 对外调用方法拦截器
 */
@Aspect
@Component
public class IntegrationAspect {
    // 信息日志格式，traceId|rpcId|目标服务.目标方法|国家|语言|耗时|是否成功|错误码|错误信息|入参|返回结果|是否压测
    private static final String INFO_PATTERN  = "%s|\"|%s|\"|%s.%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s";
    // 异常日志格式，traceId|rpcId|目标服务.目标方法|国家|语言|异常码|异常信息|是否压测
    private static final String ERROR_PATTERN = "%s|\"|%s|\"|%s.%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|";

    private final Logger logger;

    public IntegrationAspect(BizLogProperties properties) {
        this.logger = LoggerFactory.getLogger(properties.getIntegrationLoggerName());
    }

    @Around("@annotation(integration)")
    public Object around(ProceedingJoinPoint joinPoint, Integration integration) throws Throwable {
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            if (result == null && returnType != void.class && !integration.nullableResult()) {
                // 如果此方法不允许返回 null，抛出异常
                throw new ZalException("NOT_FOUND", "Could not find target.");
            } else {
                // 打印 info 日志
                printInfoLog(joinPoint, startTime, true, result, null, null);
                return result;
            }

        } catch (Throwable e) {
            // 获取错误码
            String errorCode;
            if (e instanceof ZalException) {
                errorCode = ((ZalException) e).getErrorCode();
            } else {
                errorCode = "SYSTEM_ERROR";
            }

            // IErrorCodeException 视为成功，打印普通日志，其他异常视为失败，打印错误日志
            if (e instanceof ZalException) {
                printInfoLog(joinPoint, startTime, true, null, errorCode, e.getMessage());
            } else {
                printInfoLog(joinPoint, startTime, false, null, errorCode, e.getMessage());
                printErrorLog(joinPoint, errorCode, e.getMessage(), e);
            }

            // 如果此方法允许返回 null，吞掉异常返回空值，否则重新抛出异常
            if (returnType != void.class && integration.nullableResult()) {
                return emptyValue(returnType);
            } else {
                throw e;
            }
        }
    }

    private Object emptyValue(Class<?> returnType) {
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == char.class) {
            return (char) 0;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return null;
    }

    private void printInfoLog(
        JoinPoint joinPoint, long startTime, boolean success, Object result, String errorCode, String errorMsg
    ) {
        logger.info(
            String.format(INFO_PATTERN,
                EagleEye.getTraceId(),
                EagleEye.getRpcId(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                LocalizationUtils.getLocale().getCountry(),
                LocalizationUtils.getLocale().getLanguage(),
                System.currentTimeMillis() - startTime,
                ObjectFormatUtils.booleanToChar(success),
                errorCode,
                errorMsg,
                ObjectFormatUtils.arrayToString(joinPoint.getArgs()),
                ObjectFormatUtils.objectToString(result),
                StressTestingUtil.isTestFlow()
            )
        );
    }

    private void printErrorLog(JoinPoint joinPoint, String errorCode, String errorMsg, Throwable throwable) {
        logger.error(
            String.format(ERROR_PATTERN,
                EagleEye.getTraceId(),
                EagleEye.getRpcId(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                LocalizationUtils.getLocale().getCountry(),
                LocalizationUtils.getLocale().getLanguage(),
                errorCode,
                errorMsg,
                StressTestingUtil.isTestFlow()
            ),
            throwable
        );
    }
}