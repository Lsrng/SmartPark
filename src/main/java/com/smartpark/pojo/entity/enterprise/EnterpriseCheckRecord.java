package com.smartpark.pojo.entity.enterprise;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_check_record")
@Schema(name = "EnterpriseCheckRecord", description = "校验记录")
public class EnterpriseCheckRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "入驻申请ID")
    private Long registerId;

    @Schema(description = "校验项ID")
    private Long checkItemId;

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "校验状态：PENDING-待校验、PASSED-通过、FAILED-未通过")
    private String checkStatus;

    @Schema(description = "校验结果详情（JSON）")
    private String checkResult;

    @Schema(description = "校验人ID")
    private Long checkedBy;

    @Schema(description = "校验时间")
    private LocalDateTime checkedAt;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
