package com.smartpark.pojo.entity.enterprise;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_type_check")
@Schema(name = "EnterpriseTypeCheck", description = "企业类型-校验项关联")
public class EnterpriseTypeCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "企业类型ID")
    private Long typeId;

    @Schema(description = "校验项ID")
    private Long checkItemId;

    @Schema(description = "步骤序号（从1开始）")
    private Integer stepOrder;

    @Schema(description = "状态：ENABLED-启用、DISABLED-禁用")
    private String status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
