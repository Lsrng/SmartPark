package com.smartpark.pojo.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "入驻进度视图")
public class ProgressVO {

    @Schema(description = "入驻申请ID")
    private Long registerId;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "入驻状态")
    private String status;

    @Schema(description = "当前步骤序号")
    private Integer currentStep;

    @Schema(description = "总步骤数")
    private Integer totalSteps;

    @Schema(description = "各步骤详情")
    private List<StepInfoVO> steps;
}
