package com.smartpark.common.enumeration;

import lombok.Getter;

@Getter
public enum RegisterStatus {

    DRAFT("DRAFT", "草稿"),
    CHECKING("CHECKING", "校验中"),
    ALL_CHECKED("ALL_CHECKED", "全部校验通过"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String name;

    RegisterStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
