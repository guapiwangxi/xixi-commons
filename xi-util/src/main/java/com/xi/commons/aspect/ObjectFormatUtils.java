package com.xi.commons.aspect;

import com.alibaba.fastjson.JSON;
import com.taobao.eagleeye.EagleEye;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

/**
 * Created by vince on Apr 13, 2021.
 */
@Slf4j
class ObjectFormatUtils {

    public static String arrayToString(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        return Arrays.stream(args)
            .map(ObjectFormatUtils::objectToString)
            .collect(joining(","));
    }

    public static String objectToString(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            return JSON.toJSONString(obj);
        } catch (Throwable t) {
            log.error(String.format("%s|\"|%s|\"|%s", EagleEye.getTraceId(), EagleEye.getRpcId(), "Format json error"), t);
            return "FORMAT_JSON_ERROR";
        }
    }

    public static char booleanToChar(boolean b) {
        return b ? 'Y' : 'N';
    }
}
