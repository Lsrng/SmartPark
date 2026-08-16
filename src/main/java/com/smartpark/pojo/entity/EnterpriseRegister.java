package com.smartpark.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_register")
@Schema(name = "EnterpriseRegister", description = "入驻申请实体")
public class EnterpriseRegister implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "统一社会信用代码")
    private String unifiedCode;

    @Schema(description = "企业类型ID")
    private Long typeId;

    @Schema(description = "当前步骤序号")
    private Integer currentStep;

    @Schema(description = "入驻开始时锁定的配置版本（快照）")
    private Integer configVersion;

    @Schema(description = "状态：DRAFT/CHECKING/ALL_CHECKED/PENDING_REVIEW/APPROVED/REJECTED/EXPIRED")
    private String status;

    @Schema(description = "申请过期时间，创建时设置为当前时间+30天")
    private LocalDateTime expireAt;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
