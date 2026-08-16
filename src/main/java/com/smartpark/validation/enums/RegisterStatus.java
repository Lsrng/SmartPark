package com.smartpark.validation.enums;

import lombok.Getter;

@Getter
public enum RegisterStatus {
    DRAFT("DRAFT", "草稿"),
    CHECKING("CHECKING", "校验中"),
    ALL_CHECKED("ALL_CHECKED", "全部校验通过"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String desc;

    RegisterStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
