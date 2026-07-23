package com.smartpark.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("property_bill")
@Schema(name = "PropertyBill", description = "物业账单实体")
public class PropertyBill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "楼栋号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String buildingNo;

    @Schema(description = "单元号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unitNo;

    @Schema(description = "房号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roomNo;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "费用类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String feeType;

    @Schema(description = "计费起始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billingStartDate;

    @Schema(description = "计费截止日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billingEndDate;

    @Schema(description = "应收金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amountDue;

    @Schema(description = "已收金额")
    private BigDecimal amountPaid;

    @Schema(description = "缴费截止日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dueDate;

    @Schema(description = "账单状态：UNPAID-未缴、PAID-已缴、PARTIAL-部分缴")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
