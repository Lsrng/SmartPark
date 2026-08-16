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
@TableName("validation_config")
@Schema(name = "ValidationConfig", description = "校验配置实体")
public class ValidationConfig implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "企业类型ID")
    private Long enterpriseTypeId;

    @Schema(description = "业务策略标识符（如 BUSINESS_LICENSE）")
    private String strategyId;

    @Schema(description = "步骤序号（从1开始，连续递增）")
    private Integer stepOrder;

    @Schema(description = "配置版本号")
    private Integer configVersion;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
