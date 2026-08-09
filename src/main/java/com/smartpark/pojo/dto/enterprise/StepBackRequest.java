package com.smartpark.pojo.dto.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "回退步骤请求")
public class StepBackRequest {

    @NotNull(message = "注册ID不能为空")
    @Schema(description = "入驻申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long registerId;

    @Schema(description = "目标步骤序号（不传则回退到上一步）")
    private Integer targetStep;
}
