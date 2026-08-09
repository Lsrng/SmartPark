package com.smartpark.pojo.entity.enterprise;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_register")
@Schema(name = "EnterpriseRegister", description = "企业入驻申请")
public class EnterpriseRegister implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "企业类型ID")
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

    @Schema(description = "当前进行到的步骤序号")
    private Integer currentStep;

    @Schema(description = "入驻状态")
    private String status;

    @Schema(description = "各步骤草稿数据（JSON）")
    private String draftData;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
