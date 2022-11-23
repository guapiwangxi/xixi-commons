package com.xi.commons.aspect;

import com.alibaba.ecommerce.error.ErrorCode;
import com.alibaba.ecommerce.module.PagedResponse;
import com.alibaba.ecommerce.module.Response;
import com.alibaba.global.common.utils.EnvUtils;
import com.alibaba.global.common.utils.IPUtils;

import com.xi.commons.aspect.advice.Facade;
import com.xi.commons.logger.BizLogProperties;
import com.xi.commons.util.LocalizationUtils;
import com.xi.commons.util.RequestUtils;
import com.xi.commons.util.ZalException;
import com.xi.commons.util.ZalResponse;
import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import com.taobao.eagleeye.EagleEye;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * facade 方法拦截器
 */
@Aspect
@Component
public class FacadeAspect {
    // facade日志格式：traceId|rpcId|接口名.方法名|FACADE类型|agent信息|utdId|国家|语言|耗时|是否成功|错误码|错误信息|入参|返回结果|是否压测|玩法类型
    private static final String FACADE_LOG_PATTERN       = "%s|\"|%s|\"|%s.%s|\"|HSF|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s";
    // facade-error日志格式：traceId|rpcId|接口名.方法名|FACADE类型|agent信息|utdId|国家|语言|错误码|错误信息|是否压测
    private static final String FACADE_ERROR_LOG_PATTERN = "%s|\"|%s|\"|%s.%s|\"|HSF|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s|\"|%s";

    private final Logger logger;

    public FacadeAspect(BizLogProperties properties) {
        this.logger = LoggerFactory.getLogger(properties.getFacadeLoggerName());
    }

    @Around("@within(facade)")
    public Object around(ProceedingJoinPoint joinPoint, Facade facade) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // 正常流程，调用接口，打印 info 日志
            Object result = joinPoint.proceed();
            printInfoLog(joinPoint, facade, startTime, true, result, null, null);
            return result;

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
                printInfoLog(joinPoint, facade, startTime, true, null, errorCode, e.getMessage());
            } else {
                printInfoLog(joinPoint, facade, startTime, false, null, errorCode, e.getMessage());
                printErrorLog(joinPoint, errorCode, e.getMessage(), e);
            }

            // 如果接口返回类型是 Response，把错误码包装成相应类型返回，否则重新抛出异常
            Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

            if (returnType.isAssignableFrom(Response.class)) {
                return Response.failed(ErrorCode.frontEndCode(errorCode, e.getMessage()));
            }

            if (returnType.isAssignableFrom(PagedResponse.class)) {
                return PagedResponse.failed(ErrorCode.frontEndCode(errorCode, e.getMessage()));
            }

            if (returnType.isAssignableFrom(ZalResponse.class)) {
                return ZalResponse.failed(errorCode, e.getMessage());
            }

            throw e;
        }
    }

    private void printInfoLog(
        JoinPoint joinPoint, Facade facade, long startTime, boolean success, Object result, String errorCode, String errorMsg
    ) {
        logger.info(
            String.format(FACADE_LOG_PATTERN,
                EagleEye.getTraceId(),
                EagleEye.getRpcId(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                StringUtils.defaultIfBlank(RequestUtils.getUserAgent(), "server"),
                StringUtils.defaultIfBlank(RequestUtils.getUtdid(), ""),
                LocalizationUtils.getLocale().getCountry(),
                LocalizationUtils.getLocale().getLanguage(),
                System.currentTimeMillis() - startTime,
                ObjectFormatUtils.booleanToChar(success),
                errorCode,
                errorMsg,
                ObjectFormatUtils.arrayToString(joinPoint.getArgs()),
                EnvUtils.isLive() ? "RESPONSE_NOT_PRINTED" : ObjectFormatUtils.objectToString(result),
                StressTestingUtil.isTestFlow(),
                facade.bizCode(),
                IPUtils.getLocalIp()
            )
        );
    }

    private void printErrorLog(JoinPoint joinPoint, String errorCode, String errorMsg, Throwable throwable) {
        logger.error(
            String.format(FACADE_ERROR_LOG_PATTERN,
                EagleEye.getTraceId(),
                EagleEye.getRpcId(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                StringUtils.defaultIfBlank(RequestUtils.getUserAgent(), "server"),
                StringUtils.defaultIfBlank(RequestUtils.getUtdid(), ""),
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