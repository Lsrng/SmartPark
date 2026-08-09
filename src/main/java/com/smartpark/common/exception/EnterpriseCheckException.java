package com.smartpark.common.exception;

import lombok.Getter;

@Getter
public class EnterpriseCheckException extends RuntimeException {

    private final String code;

    public EnterpriseCheckException(String message) {
        super(message);
        this.code = "CHECK_FAILED";
    }

    public EnterpriseCheckException(String code, String message) {
        super(message);
        this.code = code;
    }

    public EnterpriseCheckException(String message, Throwable cause) {
        super(message, cause);
        this.code = "CHECK_ERROR";
    }
}
