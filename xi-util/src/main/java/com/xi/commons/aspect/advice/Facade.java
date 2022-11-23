package com.xi.commons.aspect.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在 facade 实现类上的注解，此注解有两个功能：
 *
 * 1. 拦截所有接口，打印 facade.log 日志，发生异常时，打印 facade-error.log 日志
 * 2. 捕捉所有接口抛出的异常，包装成合适的 Response 对象返回，避免直接把异常抛到 HSF 框架层
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Facade {

    /**
     * 玩法类型，用于在 facade.log 日志中区分不同玩法的流量
     */
    String bizCode() default "default";
}