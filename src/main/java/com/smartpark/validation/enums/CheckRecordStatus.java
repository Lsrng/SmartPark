package com.smartpark.validation.enums;

import lombok.Getter;

@Getter
public enum CheckRecordStatus {
    PENDING("PENDING", "待校验"),
    PASSED("PASSED", "已通过"),
    FAILED("FAILED", "未通过");

    private final String code;
    private final String desc;

    CheckRecordStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
