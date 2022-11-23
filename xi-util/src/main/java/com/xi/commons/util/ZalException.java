package com.xi.commons.util;

/**
 * @author qian.lqlq
 * @version $Id: ZalException.java, v 0.1 2019年07月06日 14:48 qian.lqlq Exp $
 */
public class ZalException extends RuntimeException {
    private static final long serialVersionUID = -5263614729617505963L;
    private final String errorCode;

    public ZalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}