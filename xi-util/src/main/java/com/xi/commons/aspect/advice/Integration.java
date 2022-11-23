package com.xi.commons.aspect.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 此注解用于标注服务集成层方法（一般功能是包装外部依赖服务的调用），
 * 打印方法的入参出参数据到 integration.log，如果发生异常，打印 integration-error.log
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Integration {

    /**
     * 是否允许方法返回值为 null，默认为 false
     *
     * 当设置为 false 时：
     * 1. 如果方法返回值为 null，会抛出 ZalException，错误码为 NOT_FOUND
     *
     * 当设置为 true 时：
     * 1. 如果方法返回值为 null，会直接返回
     * 2. 如果方法发生异常，会吞掉异常，直接返回 null（若方法返回值为基本类型，返回该类型的默认值，如 int 则返回 0）
     */
    boolean nullableResult() default false;
}