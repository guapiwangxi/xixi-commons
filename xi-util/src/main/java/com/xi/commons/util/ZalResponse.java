package com.xi.commons.util;

import java.io.Serializable;
import java.util.List;

public class ZalResponse<T> implements Serializable {

    private static final long serialVersionUID = 6688502770504094290L;

    private T                 module;

    private boolean           isSuccess;

    private ErrorCode         errorCode;

    private List<String>      dataTracks;

    private ZalResponse(boolean isSuccess, T module) {
        this.isSuccess = isSuccess;
        this.module = module;
    }

    private ZalResponse(String code, String msg) {
        this.isSuccess = false;
        this.errorCode = this.new ErrorCode(code, msg);
    }

    public static <T> ZalResponse<T> success() {
        return new ZalResponse<>(true, null);
    }

    public static <T> ZalResponse<T> success(T module) {
        return new ZalResponse<>(true, module);
    }

    public static <T> ZalResponse<T> failed(String code, String msg) {
        return new ZalResponse<>(code, msg);
    }

    public T getModule() {
        return this.module;
    }

    public boolean isSuccess() {
        return this.isSuccess;
    }

    public boolean isNotSuccess() {
        return !this.isSuccess;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDataTracks() {
        return this.dataTracks;
    }

    public void setAbTestDataTrack(List<String> dataTracks) {
        this.dataTracks = dataTracks;
    }

    public class ErrorCode {

        private String key;

        private String displayMessage;

        public ErrorCode(String key, String displayMessage) {
            this.key = key;
            this.displayMessage = displayMessage;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayMessage() {
            return displayMessage;
        }
    }
}