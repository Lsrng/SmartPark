package com.smartpark.pojo.dto.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "提交步骤校验请求")
public class StepSubmitRequest {

    @NotNull(message = "注册ID不能为空")
    @Schema(description = "入驻申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long registerId;

    @NotNull(message = "步骤序号不能为空")
    @Schema(description = "步骤序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stepOrder;

    @Schema(description = "提交的表单数据")
    private Map<String, Object> formData;
}
