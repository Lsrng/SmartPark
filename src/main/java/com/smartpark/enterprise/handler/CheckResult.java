package com.smartpark.enterprise.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 校验结果
 */
@Data
@Builder
@AllArgsConstructor
public class CheckResult {

    /** 是否通过 */
    private boolean passed;

    /** 校验结果描述 */
    private String message;

    /** 校验详情（JSON字符串，可包含具体字段的校验结果） */
    private String detail;

    /** 创建成功结果 */
    public static CheckResult success(String message) {
        return CheckResult.builder().passed(true).message(message).build();
    }

    public static CheckResult success(String message, String detail) {
        return CheckResult.builder().passed(true).message(message).detail(detail).build();
    }

    /** 创建失败结果 */
    public static CheckResult failed(String message) {
        return CheckResult.builder().passed(false).message(message).build();
    }

    public static CheckResult failed(String message, String detail) {
        return CheckResult.builder().passed(false).message(message).detail(detail).build();
    }
}
