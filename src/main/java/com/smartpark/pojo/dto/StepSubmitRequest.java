package com.smartpark.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "StepSubmitRequest", description = "步骤提交请求")
public class StepSubmitRequest {

    @Schema(description = "入驻申请ID")
    private Long registerId;

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "用户提交的表单数据")
    private Map<String, Object> formData;
}
