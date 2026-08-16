package com.smartpark.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "StepInfoVO", description = "步骤信息视图对象")
public class StepInfoVO {

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "策略标识符")
    private String strategyId;

    @Schema(description = "策略名称")
    private String strategyName;

    @Schema(description = "状态：PENDING/PASSED/FAILED")
    private String status;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
