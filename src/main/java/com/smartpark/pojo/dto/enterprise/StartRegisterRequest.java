package com.smartpark.pojo.dto.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "开始入驻请求")
public class StartRegisterRequest {

    @NotBlank(message = "企业名称不能为空")
    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseName;

    @NotNull(message = "企业类型不能为空")
    @Schema(description = "企业类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long typeId;

    @Schema(description = "统一社会信用代码")
    private String unifiedCode;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "法人联系电话")
    private String legalPersonPhone;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "企业地址")
    private String address;
}
