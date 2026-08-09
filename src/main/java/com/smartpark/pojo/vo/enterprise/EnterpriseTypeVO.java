package com.smartpark.pojo.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "企业类型视图")
public class EnterpriseTypeVO {

    @Schema(description = "企业类型ID")
    private Long id;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "类型编码")
    private String code;

    @Schema(description = "类型描述")
    private String description;

    @Schema(description = "总校验步骤数")
    private Integer totalSteps;
}
