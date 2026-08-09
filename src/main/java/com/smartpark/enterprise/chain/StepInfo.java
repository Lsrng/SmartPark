package com.smartpark.enterprise.chain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 步骤元信息
 */
@Data
@AllArgsConstructor
public class StepInfo {

    /** 步骤序号 */
    private Integer stepOrder;

    /** 步骤名称 */
    private String stepName;

    /** 步骤编码 */
    private String stepCode;

    /** 校验项ID */
    private Long checkItemId;
}
