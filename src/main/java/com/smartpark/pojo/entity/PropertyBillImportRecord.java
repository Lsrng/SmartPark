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
@TableName("property_bill_import_record")
@Schema(name = "PropertyBillImportRecord", description = "物业账单导入记录实体")
public class PropertyBillImportRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务唯一标识（UUID）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "总行数（不含表头）")
    private Integer totalRows;

    @Schema(description = "成功导入行数")
    private Integer successRows;

    @Schema(description = "失败行数")
    private Integer failRows;

    @Schema(description = "失败行明细，JSON数组格式")
    private String failDetail;

    @Schema(description = "任务状态：PENDING-排队中、PROCESSING-处理中、SUCCESS-成功、FAIL-失败")
    private String status;

    @Schema(description = "处理耗时（毫秒）")
    private Long costTimeMs;

    @Schema(description = "操作人ID")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
