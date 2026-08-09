package com.smartpark.pojo.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "步骤信息视图")
public class StepInfoVO {

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "步骤编码")
    private String stepCode;

    @Schema(description = "校验状态：PENDING-待校验、PASSED-通过、FAILED-未通过")
    private String checkStatus;

    @Schema(description = "校验结果详情")
    private String checkResult;

    @Schema(description = "是否当前步骤")
    private Boolean isCurrent;

    @Schema(description = "是否已解锁（前置步骤已通过）")
    private Boolean unlocked;
}
