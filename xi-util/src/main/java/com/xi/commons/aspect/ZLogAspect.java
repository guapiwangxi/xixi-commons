package com.xi.commons.aspect;

import com.alibaba.ecommerce.module.PagedResponse;
import com.alibaba.ecommerce.module.Response;

import com.xi.commons.aspect.advice.UseZLog;
import com.xi.commons.util.LocalizationUtils;
import com.xi.commons.util.RequestUtils;
import com.xi.commons.util.ZalException;
import com.xi.commons.logger.ZLog;
import com.xi.commons.util.ZalResponse;
import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import com.taobao.eagleeye.EagleEye;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 拦截 UseZLog 注解，打印业务日志
 */
@Aspect
@Component
public class ZLogAspect {

    @Around("@annotation(useZLog)")
    public Object around(ProceedingJoinPoint joinPoint, UseZLog useZLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        ZLog.start(useZLog.logger(), useZLog.collectNestedLogs());

        try {
            // 记录通用数据
            Signature signature = joinPoint.getSignature();
            ZLog.ap("siteId", LocalizationUtils.getLocale().getCountry())
                .ap("bizType", signature.getDeclaringType().getSimpleName() + "." + signature.getName())
                .ap("traceId", EagleEye.getTraceId())
                .ap("utdid", RequestUtils.getUtdid())
                .ap("umid", RequestUtils.getUmid())
                .ap("userId", RequestUtils.getUserId())
                .ap("userIp", RequestUtils.getIp())
                .ap("stress", StressTestingUtil.isTestFlow());

            Object result = joinPoint.proceed();

            // 如果方法返回值为 ZalResponse 并且不成功，记录错误信息
            if (result instanceof ZalResponse) {
                ZalResponse<?> r = (ZalResponse<?>) result;
                if (r.isNotSuccess()) {
                    ZLog.ap("error", r.getErrorCode().getKey()).ap("errorMsg", r.getErrorCode().getDisplayMessage());
                }
            }

            // 如果方法返回值为 Response 并且不成功，记录错误信息
            if (result instanceof Response) {
                Response<?> r = (Response<?>) result;
                if (r.isNotSuccess()) {
                    ZLog.ap("error", r.getErrorCode().getKey()).ap("errorMsg", r.getErrorCode().getDisplayMessage());
                }
            }

            // 如果方法返回值为 PagedResponse 并且不成功，记录错误信息
            if (result instanceof PagedResponse) {
                PagedResponse<?> r = (PagedResponse<?>) result;
                if (!r.isSuccess()) {
                    ZLog.ap("error", r.getErrorCode().getKey()).ap("errorMsg", r.getErrorCode().getDisplayMessage());
                }
            }

            return result;

        } catch (Throwable e) {
            // 捕捉到异常，记录异常信息
            if (e instanceof ZalException) {
                ZLog.ap("error", ((ZalException) e).getErrorCode()).ap("errorMsg", e.getMessage());
            } else {
                ZLog.ap("error", "SYSTEM_ERROR").ap("errorMsg", e.getMessage());
            }

            // 把异常包装为 Response 是 @Facade 注解的职责，这里不作处理，直接抛出即可
            throw e;

        } finally {
            ZLog.ap("spentTime", System.currentTimeMillis() - startTime);
            ZLog.end(useZLog.level());
        }
    }
}
