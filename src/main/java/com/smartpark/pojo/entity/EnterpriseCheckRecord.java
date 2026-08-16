package com.smartpark.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "enterprise_check_record", autoResultMap = true)
@Schema(name = "EnterpriseCheckRecord", description = "校验记录实体")
public class EnterpriseCheckRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "入驻申请ID")
    private Long registerId;

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "业务策略标识符")
    private String strategyId;

    @Schema(description = "状态：PENDING/PASSED/FAILED")
    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "用户提交的表单数据")
    private Map<String, Object> formData;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "校验结果详情")
    private Map<String, Object> checkResult;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误描述")
    private String errorMessage;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
