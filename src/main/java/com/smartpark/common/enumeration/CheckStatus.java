package com.smartpark.common.enumeration;

import lombok.Getter;

@Getter
public enum CheckStatus {

    PENDING("PENDING", "待校验"),
    PASSED("PASSED", "通过"),
    FAILED("FAILED", "未通过");

    private final String code;
    private final String name;

    CheckStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
