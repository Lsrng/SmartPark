package com.smartpark.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "StartRegisterRequest", description = "创建入驻申请请求")
public class StartRegisterRequest {

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "统一社会信用代码")
    private String unifiedCode;

    @Schema(description = "企业类型ID")
    private Long typeId;
}
