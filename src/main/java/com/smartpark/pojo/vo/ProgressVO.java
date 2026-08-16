package com.smartpark.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProgressVO", description = "入驻进度视图对象")
public class ProgressVO {

    @Schema(description = "入驻申请ID")
    private Long registerId;

    @Schema(description = "当前步骤序号")
    private Integer currentStep;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "各步骤详情")
    private List<StepInfoVO> steps;
}
